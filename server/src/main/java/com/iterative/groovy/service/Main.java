package com.iterative.groovy.service;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		GroovyService service = new GroovyShellService(6789);
		service.launch();
	}
}
