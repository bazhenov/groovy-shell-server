package com.iterative.groovy.service;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.io.IOException;
import java.util.Arrays;

public class Main {

	public static void main(String[] args) throws IOException {
		/*GroovyShellService service = new GroovyShellService();
		service.setPort(6789);
		service.start();*/

		SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setPort(2200);
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
		NamedFactory<UserAuth> a = new UserAuthNone.Factory();
		sshd.setUserAuthFactories(Arrays.asList(a));
		sshd.setShellFactory(new MyShellFactory());
		sshd.start();
	}

}
