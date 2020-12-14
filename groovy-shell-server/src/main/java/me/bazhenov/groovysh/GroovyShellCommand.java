package me.bazhenov.groovysh;

import groovy.lang.Binding;
import groovy.lang.Closure;
import me.bazhenov.groovysh.thread.ServerSessionAwareThreadFactory;
import org.apache.groovy.groovysh.Groovysh;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.session.helpers.AbstractSession;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.session.ServerSession;
import org.codehaus.groovy.tools.shell.IO;

import java.io.*;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static me.bazhenov.groovysh.GroovyShellService.SHELL_KEY;

class GroovyShellCommand implements Command, SessionAware {

	private final SshServer sshd;
	private final Map<String, Object> bindings;
	private final List<String> defaultScripts;
	private final ServerSessionAwareThreadFactory threadFactory;
	private InputStream in;
	private OutputStream out;
	private OutputStream err;
	private ExitCallback callback;
	private Thread wrapper;
	private ServerSession session;

	GroovyShellCommand(
		SshServer sshd,
		Map<String, Object> bindings,
		List<String> defaultScripts,
		ServerSessionAwareThreadFactory threadFactory) {
		this.sshd = sshd;
		this.bindings = bindings;
		this.defaultScripts = defaultScripts;
		this.threadFactory = threadFactory;
	}

	private static PrintStream createPrintStream(OutputStream out)
		throws UnsupportedEncodingException {
		return new PrintStream(out, true, "utf8");
	}

	@Override
	public void setSession(ServerSession session) {
		this.session = session;
	}

	@Override
	public void setInputStream(InputStream in) {
		this.in = in;
	}

	@Override
	public void setOutputStream(OutputStream out) {
		this.out = createSafeWrapper(out);
	}

	@Override
	public void setErrorStream(OutputStream err) {
		this.err = createSafeWrapper(err);
	}

	@Override
	public void setExitCallback(ExitCallback callback) {
		this.callback = callback;
	}

	@Override
	public void start(final Environment env) throws IOException {
		TtyFilterOutputStream out = new TtyFilterOutputStream(this.out);
		TtyFilterOutputStream err = new TtyFilterOutputStream(this.err);

		IO io = new IO(in, out, err);
		io.setVerbosity(IO.Verbosity.DEBUG);
		Groovysh shell = new Groovysh(createBinding(bindings, out, err), io);
		shell.setErrorHook(
			new Closure<Object>(this) {
				@Override
				public Object call(Object... args) {
					if (args[0] instanceof InterruptedIOException || args[0] instanceof SshException) {
						// Stopping groovysh thread in case of broken client channel
						shell.getRunner().setRunning(false);
					}
					return shell.getDefaultErrorHook().call(args);
				}
			});

		try {
			loadDefaultScripts(shell);
		} catch (Exception e) {
			createPrintStream(err)
				.println(
					"Unable to load default scripts: " + e.getClass().getName() + ": " + e.getMessage());
		}

		session.setAttribute(SHELL_KEY, shell);

		wrapper =
			threadFactory.newThread(
				() -> {
					try {
						SshTerminal.registerEnvironment(env);
						shell.run("");
						callback.onExit(0);
					} catch (RuntimeException | Error e) {
						callback.onExit(-1, e.getMessage());
					}
				},
				session);
		wrapper.start();
	}

	private Binding createBinding(Map<String, Object> objects, OutputStream out, OutputStream err)
		throws UnsupportedEncodingException {
		Binding binding = new Binding();

		if (objects != null)
			for (Map.Entry<String, Object> row : objects.entrySet())
				binding.setVariable(row.getKey(), row.getValue());

		binding.setVariable("out", createPrintStream(out));
		binding.setVariable("err", createPrintStream(err));
		binding.setVariable(
			"activeSessions",
			new Closure<List<AbstractSession>>(this) {
				@Override
				public List<AbstractSession> call() {
					return sshd.getActiveSessions();
				}
			});

		return binding;
	}

	@SuppressWarnings({"unchecked", "serial"})
	private void loadDefaultScripts(Groovysh shell) {
		if (!defaultScripts.isEmpty()) {
			Closure<Groovysh> defaultResultHook = shell.getResultHook();

			try {
				// Set a "no-op closure so we don't get per-line value output when evaluating the default
				// script
				shell.setResultHook(
					new Closure<Groovysh>(this) {
						@Override
						public Groovysh call(Object... args) {
							return shell;
						}
					});

				org.apache.groovy.groovysh.Command cmd = shell.getRegistry().find(":load");
				for (String script : defaultScripts) {
					cmd.execute(singletonList(script));
				}
			} finally {
				// Restoring original result hook
				shell.setResultHook(defaultResultHook);
			}
		}
	}

	@Override
	public void destroy() {
		wrapper.interrupt();
	}

	/**
	 * Метод защищает нас от записи в закрытый канал
	 */
	private OutputStream createSafeWrapper(OutputStream out) {

		return new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				if (somethingBroken()) {
					wrapper.interrupt();
					return;
				}
				out.write(b);
			}

			private boolean somethingBroken() {
				return session.isClosed() || session.isClosing() || sshd.isClosed() || sshd.isClosing();
			}
		};
	}
}
