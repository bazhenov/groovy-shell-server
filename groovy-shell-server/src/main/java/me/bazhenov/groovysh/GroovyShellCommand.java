package me.bazhenov.groovysh;

import groovy.lang.Binding;
import groovy.lang.Closure;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

import java.io.*;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

class GroovyShellCommand implements Command {

	private final SshServer sshd;
	private final Map<String, Object> bindings;
	private final List<String> defaultScripts;
	private InputStream in;
	private OutputStream out;
	private OutputStream err;
	private ExitCallback callback;
	private Thread wrapper;

	public GroovyShellCommand(SshServer sshd, Map<String, Object> bindings, List<String> defaultScripts) {
		this.sshd = sshd;
		this.bindings = bindings;
		this.defaultScripts = defaultScripts;
	}

	@Override
	public void setInputStream(InputStream in) {
		this.in = in;
	}

	@Override
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}

	@Override
	public void setErrorStream(OutputStream err) {
		this.err = err;
	}

	@Override
	public void setExitCallback(ExitCallback callback) {
		this.callback = callback;
	}

	@Override
	public void start(final Environment env) throws IOException {
		TtyFilterOutputStream out = new TtyFilterOutputStream(this.out);
		TtyFilterOutputStream err = new TtyFilterOutputStream(this.err);

		final Groovysh shell = new Groovysh(createBinding(bindings, out, err), new IO(in, out, err));

		loadDefaultScripts(shell);

		String threadName = "GroovySh Client Thread";
		wrapper = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					SshTerminal.registerEnvironment(env);
					shell.run();
					callback.onExit(0);
				} catch (RuntimeException e) {
					callback.onExit(-1, e.getMessage());
				} catch (Error e) {
					callback.onExit(-1, e.getMessage());
				}
			}
		}, threadName);
		wrapper.start();
	}

	private Binding createBinding(Map<String, Object> objects, OutputStream out, OutputStream err)
		throws UnsupportedEncodingException {
		Binding binding = new Binding();

		if (objects != null)
			for (Map.Entry<String, Object> row : objects.entrySet())
				binding.setVariable(row.getKey(), row.getValue());

		binding.setVariable("out", new PrintStream(out, true, "utf8"));
		binding.setVariable("err", new PrintStream(err, true, "utf8"));
		binding.setVariable("activeSessions", new Closure<List<AbstractSession>>(this) {
			@Override
			public List<AbstractSession> call() {
				return sshd.getActiveSessions();
			}
		});

		return binding;
	}

	@SuppressWarnings({"unchecked", "serial"})
	private void loadDefaultScripts(final Groovysh shell) {
		if (!defaultScripts.isEmpty()) {
			Closure<Groovysh> defaultResultHook = shell.getResultHook();

			try {
				// Set a "no-op closure so we don't get per-line value output when evaluating the default script
				shell.setResultHook(new Closure<Groovysh>(this) {
					@Override
					public Groovysh call(Object... args) {
						return shell;
					}
				});

				org.codehaus.groovy.tools.shell.Command cmd = shell.getRegistry().find("load");
				for (String script : defaultScripts) {
					cmd.execute(asList(script));
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
}
