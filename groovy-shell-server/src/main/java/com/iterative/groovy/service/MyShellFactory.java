package com.iterative.groovy.service;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;

class MyShellFactory implements Factory<Command> {

	@Override
	public Command create() {
		return new GroovyShellCommand();
	}
}
