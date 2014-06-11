package me.bazhenov.groovysh;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		GroovyShellService service = new GroovyShellService();
		service.setPort(6789);
		service.start();
	}
}
