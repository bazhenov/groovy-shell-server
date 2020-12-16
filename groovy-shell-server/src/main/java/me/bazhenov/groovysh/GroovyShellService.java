/*
 * Copyright 2007 Bruce Fancher
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package me.bazhenov.groovysh;

import me.bazhenov.groovysh.thread.DefaultGroovyshThreadFactory;
import me.bazhenov.groovysh.thread.ServerSessionAwareThreadFactory;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.groovy.groovysh.Groovysh;
import org.codehaus.groovy.tools.shell.util.Preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.HOURS;
import static jline.TerminalFactory.Flavor.UNIX;
import static jline.TerminalFactory.registerFlavor;
import static org.apache.sshd.common.FactoryManager.IDLE_TIMEOUT;
import static org.apache.sshd.common.PropertyResolverUtils.updateProperty;
import static org.apache.sshd.server.SshServer.setUpDefaultServer;
import static org.apache.groovy.groovysh.util.PackageHelper.IMPORT_COMPLETION_PREFERENCE_KEY;

/**
 * Instantiate this class and call {@link #start()} to start a GroovyShell
 *
 * @author Denis Bazhenov
 */
@SuppressWarnings("UnusedDeclaration")
public class GroovyShellService {

	private int port;
	private String host;
	private Map<String, Object> bindings;
	private PasswordAuthenticator passwordAuthenticator;
	private ServerSessionAwareThreadFactory threadFactory = new DefaultGroovyshThreadFactory();

	static final Session.AttributeKey<Groovysh> SHELL_KEY = new Session.AttributeKey<>();
	private List<String> defaultScripts = new ArrayList<>();
	private SshServer sshd;
	private boolean disableImportCompletions = false;

	/**
	 * Uses a default port of 6789
	 */
	public GroovyShellService() {
		this(6789);
	}

	@SuppressWarnings("WeakerAccess")
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

	/**
	 * Disable import completion autoscan.
	 * <p>
	 * Import completion autoscan known to cause problems on a Spring Boot applications packaged in uber-jar. Please,
	 * keep
	 * in mind that value is written (and persisted) using Java Preferences API. So once written it should be removed by
	 * hand (you can use groovysh <code>:set</code> command).
	 *
	 * @see <a href="https://github.com/bazhenov/groovy-shell-server/issues/26">groovy-shell-server does not work with
	 * jdk11</a>
	 */
	public void setDisableImportCompletions(boolean disableImportCompletions) {
		this.disableImportCompletions = disableImportCompletions;
	}

	public void setPasswordAuthenticator(PasswordAuthenticator passwordAuthenticator) {
		this.passwordAuthenticator = passwordAuthenticator;
	}

	public void setThreadFactory(ServerSessionAwareThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
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
		if (disableImportCompletions)
			Preferences.put(IMPORT_COMPLETION_PREFERENCE_KEY, "true");
	}

	private SshServer buildSshServer() {
		SshServer sshd = setUpDefaultServer();
		sshd.setPort(port);
		if (host != null) {
			sshd.setHost(host);
		}

		updateProperty(sshd, IDLE_TIMEOUT, HOURS.toMillis(8));

		sshd.addSessionListener(new SessionListener() {
			@Override
			public void sessionCreated(Session session) {
			}

			@Override
			public void sessionEvent(Session sesssion, Event event) {
			}

			@Override
			public void sessionException(Session session, Throwable t) {
			}

			@Override
			public void sessionClosed(Session session) {
				Groovysh shell = session.getAttribute(SHELL_KEY);
				if (shell != null)
					shell.getRunner().setRunning(false);
			}
		});

		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("host.key").toPath()));
		configureAuthentication(sshd);
		sshd.setShellFactory(new GroovyShellFactory());
		return sshd;
	}

	private void configureAuthentication(SshServer sshd) {
		NamedFactory<UserAuth> auth;
		if (this.passwordAuthenticator != null) {
			sshd.setPasswordAuthenticator(this.passwordAuthenticator);
			auth = new UserAuthPasswordFactory();
		} else {
			auth = new UserAuthNoneFactory();
		}
		sshd.setUserAuthFactories(singletonList(auth));
	}

	public synchronized void destroy() throws IOException {
		sshd.stop(true);
	}

	class GroovyShellFactory implements Factory<Command> {

		@Override
		public Command create() {
			return new GroovyShellCommand(sshd, bindings, defaultScripts, threadFactory);
		}
	}
}
