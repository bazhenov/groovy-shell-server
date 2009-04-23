package com.iterative.groovy.service;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		System.setProperty("line.separator", "\r\n");
		GroovyService service = new GroovyShellService(6789);
		service.launch();
	}
}
