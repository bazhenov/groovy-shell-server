package me.bazhenov.groovysh;

import java.io.IOException;

import static java.lang.Thread.currentThread;

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
