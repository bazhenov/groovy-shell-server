package me.bazhenov.groovysh;

import static java.lang.Thread.currentThread;

import java.io.IOException;

public class Main {

  @SuppressWarnings("BusyWait")
  public static void main(String[] args) throws IOException, InterruptedException {
    GroovyShellService service = new GroovyShellService();
    service.setPort(6789);
    service.start();

		while (!currentThread().isInterrupted()) {
			Thread.sleep(100);
		}
  }
}
