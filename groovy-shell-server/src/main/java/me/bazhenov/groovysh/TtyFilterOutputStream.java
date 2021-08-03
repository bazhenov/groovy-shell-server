package me.bazhenov.groovysh;

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
  private final AtomicBoolean isChannelAlive;

  TtyFilterOutputStream(OutputStream out, AtomicBoolean isChannelAlive) {
    super(out);
    this.isChannelAlive = isChannelAlive;
  }

  @Override
  public void write(int c) throws IOException {
    if (isChannelAlive.get()) {
      if (c == '\n') {
        super.write(c);
        c = '\r';
      }
      super.write(c);
    }
  }

  @Override
  public void flush() throws IOException {
    if (isChannelAlive.get()) {
      super.flush();
    }
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (isChannelAlive.get()) {
      for (int i = off; i < len; i++) {
        write(b[i]);
      }
    }
  }
}
