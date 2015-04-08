/**
 * Copyright 2007 Bruce Fancher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.bazhenov.groovysh;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SessionListener;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.SessionFactory;
import org.codehaus.groovy.tools.shell.Groovysh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.HOURS;
import static jline.TerminalFactory.Flavor.UNIX;
import static jline.TerminalFactory.registerFlavor;
import static org.apache.sshd.SshServer.setUpDefaultServer;
import static org.apache.sshd.server.ServerFactoryManager.IDLE_TIMEOUT;

/**
 * Instantiate this class and call {@link #start()} to start a GroovyShell
 *
 * @author Denis Bazhenov
 */
@SuppressWarnings("UnusedDeclaration")
public class GroovyShellService {

	public static final Session.AttributeKey<Groovysh> SHELL_KEY = new Session.AttributeKey<Groovysh>();

	private int port;
	private String host;
	private Map<String, Object> bindings;

	private List<String> defaultScripts = new ArrayList<String>();
	private SshServer sshd;

	/**
	 * Uses a default port of 6789
	 */
	public GroovyShellService() {
		this(6789);
	}

	public GroovyShellService(int port) {
		if (port <= 0 || port > 65535) {
			throw new IllegalArgumentException("Wrong port number");
		}
		this.port = port;
		registerFlavor(UNIX, SshTerminal.class);
	}

	public Map<String, Object> getBindings() {
		return bindings;
	}

	public void setBindings(Map<String, Object> bindings) {
		this.bindings = bindings;
	}

	public int getPort() {
		return port;
	}

	/**
	 * Adds a groovy script to be executed for each new client session.
	 *
	 * @param script script
	 */
	public void addDefaultScript(String script) {
		defaultScripts.add(script);
	}

	/**
	 * @return complete List of scripts to be executed for each new client session
	 */
	public List<String> getDefaultScripts() {
		return defaultScripts;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public void setDefaultScripts(List<String> defaultScriptNames) {
		this.defaultScripts = defaultScriptNames;
	}

	/**
	 * Starts Groovysh
	 *
	 * @throws IOException thrown if socket cannot be opened
	 */
	public synchronized void start() throws IOException {
		sshd = buildSshServer();
		sshd.start();
	}

	protected SshServer buildSshServer() {
		SshServer sshd = setUpDefaultServer();
		sshd.setPort(port);
		if (host != null) {
			sshd.setHost(host);
		}

		Map<String, String> properties = new HashMap<String, String>();
		properties.put(IDLE_TIMEOUT, Long.toString(HOURS.toMillis(1)));
		sshd.setProperties(properties);

		SessionFactory sessionFactory = new SessionFactory();
		sessionFactory.addListener(new SessionListener() {
			@Override
			public void sessionCreated(Session session) {
			}

			@Override
			public void sessionEvent(Session sesssion, Event event) {
			}

			@Override
			public void sessionClosed(Session session) {
				Groovysh shell = session.getAttribute(SHELL_KEY);
				if (shell != null)
					shell.getRunner().setRunning(false);
			}
		});
		sshd.setSessionFactory(sessionFactory);

		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("host.key"));
		NamedFactory<UserAuth> a = new UserAuthNone.Factory();
		//noinspection unchecked
		sshd.setUserAuthFactories(asList(a));
		sshd.setShellFactory(new GroovyShellFactory());
		return sshd;
	}

	public synchronized void destroy() throws InterruptedException {
		sshd.stop(true);
	}

	class GroovyShellFactory implements Factory<Command> {

		@Override
		public Command create() {
			return new GroovyShellCommand(sshd, bindings, defaultScripts);
		}
	}
}
