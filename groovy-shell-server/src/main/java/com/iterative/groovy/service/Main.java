package com.iterative.groovy.service;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		GroovyShellService service = new GroovyShellService();
		service.setPort(6789);
		service.start();
	}
}
