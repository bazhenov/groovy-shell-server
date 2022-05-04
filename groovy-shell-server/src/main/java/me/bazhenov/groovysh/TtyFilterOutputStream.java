package me.bazhenov.groovysh;

import org.apache.sshd.common.channel.exception.SshChannelException;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

class TtyFilterOutputStream extends FilterOutputStream {

  /**
   * There is a some cases ('groovyShell.destroy()' called from GroovyShell session for example)
   * when the result of the command cannot be displayed to the GroovyShell user, because the ssh
   * channel is already closed. The error message that has occurred cannot be displayed either. This
   * causes a stream of recursive errors to fill up application logs.
   * <p>
   * Therefore, we add here an explicit check that the ssh channel is alive, in order to extinguish
   * the SshChannelClosedException in the parent class and avoid such recursion.
   */
  private final AtomicBoolean isServiceAlive, isChannelAlive = new AtomicBoolean(true);

  TtyFilterOutputStream(OutputStream out, AtomicBoolean isServiceAlive) {
    super(out);
    this.isServiceAlive = isServiceAlive;
  }

  @Override
  public void write(int c) throws IOException {
    if (isAlive()) {
      try {
        if (c == '\n') {
          super.write(c);
          c = '\r';
        }
        super.write(c);
      } catch (SshChannelException e) {
        isChannelAlive.set(false);
        throw e;
      }
    }
  }

  @Override
  public void flush() throws IOException {
    if (isAlive()) {
      try {
        super.flush();
      } catch (SshChannelException e) {
        isChannelAlive.set(false);
        throw e;
      }
    }
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (isAlive()) {
      for (int i = off; i < len; i++) {
        write(b[i]);
      }
    }
  }

  private boolean isAlive() {
    return isServiceAlive.get() && isChannelAlive.get();
  }
}
