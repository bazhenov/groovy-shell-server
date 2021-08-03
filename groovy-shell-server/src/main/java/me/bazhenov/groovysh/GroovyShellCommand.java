package me.bazhenov.groovysh;

import static java.util.Collections.singletonList;
import static me.bazhenov.groovysh.GroovyShellService.SHELL_KEY;
import static org.codehaus.groovy.tools.shell.IO.Verbosity.DEBUG;

import groovy.lang.Binding;
import groovy.lang.Closure;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.groovy.groovysh.Groovysh;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.session.helpers.AbstractSession;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.codehaus.groovy.tools.shell.IO;

class GroovyShellCommand implements Command {

  private final SshServer sshd;
  private final Map<String, Object> bindings;
  private final List<String> defaultScripts;
  private InputStream in;
  private OutputStream out;
  private OutputStream err;
  private ExitCallback callback;
  private Thread wrapper;
  private ChannelSession session;
  private final AtomicBoolean isChannelAlive;

  GroovyShellCommand(SshServer sshd, Map<String, Object> bindings, List<String> defaultScripts,
      AtomicBoolean isChannelAlive) {
    this.sshd = sshd;
    this.bindings = bindings;
    this.defaultScripts = defaultScripts;
    this.isChannelAlive = isChannelAlive;
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
  public void start(ChannelSession session, Environment env) throws IOException {
    this.session = session;
    TtyFilterOutputStream out = new TtyFilterOutputStream(this.out, isChannelAlive);
    TtyFilterOutputStream err = new TtyFilterOutputStream(this.err, isChannelAlive);

    IO io = new IO(in, out, err);
    io.setVerbosity(DEBUG);
    Groovysh shell = new Groovysh(createBinding(bindings, out, err), io);
    shell.setErrorHook(new Closure<Object>(this) {
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
      createPrintStream(err).println("Unable to load default scripts: "
          + e.getClass().getName() + ": " + e.getMessage());
    }

    this.session.setAttribute(SHELL_KEY, shell);

    Runnable runnable = () -> {
      try {
        SshTerminal.registerEnvironment(env);
        shell.run("");
        callback.onExit(0);
      } catch (RuntimeException | Error e) {
        callback.onExit(-1, e.getMessage());
      }
    };
    wrapper = newThread(runnable, this.session);
    wrapper.start();
  }

  private static Thread newThread(Runnable r, ChannelSession session) {
    String address = session.getSession().getIoSession().getRemoteAddress().toString();
    String threadName = "GroovySh Client Thread: " + address;
    return new Thread(r, threadName);
  }

  private Binding createBinding(Map<String, Object> objects, OutputStream out, OutputStream err)
      throws UnsupportedEncodingException {
    Binding binding = new Binding();

    if (objects != null) {
      for (Map.Entry<String, Object> row : objects.entrySet()) {
        binding.setVariable(row.getKey(), row.getValue());
      }
    }

    binding.setVariable("out", createPrintStream(out));
    binding.setVariable("err", createPrintStream(err));
    binding.setVariable("activeSessions", new Closure<List<AbstractSession>>(this) {
      @Override
      public List<AbstractSession> call() {
        return sshd.getActiveSessions();
      }
    });

    return binding;
  }

  private static PrintStream createPrintStream(OutputStream out)
      throws UnsupportedEncodingException {
    return new PrintStream(out, true, "utf8");
  }

  @SuppressWarnings({"unchecked", "serial"})
  private void loadDefaultScripts(Groovysh shell) {
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
  public void destroy(ChannelSession channel) {
    wrapper.interrupt();
  }
}
