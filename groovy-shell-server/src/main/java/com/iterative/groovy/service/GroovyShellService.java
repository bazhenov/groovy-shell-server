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
package com.iterative.groovy.service;

import groovy.lang.Binding;
import org.slf4j.Logger;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Instantiate this class and call {@link #start()} to create a GroovyShell server socket
 * which can accept client connections and initiate groovysh sessions.
 * <p/>
 * <p/>Each instance of this class will register itself with the default MBean server
 * to allow for management of {@link GroovyShellServiceMBean} methods via a JMX agent.
 *
 * @author Bruce Fancher
 * @author Denis Bazhenov
 */
public class GroovyShellService implements GroovyShellServiceMBean {

	private static final Logger log = getLogger(GroovyShellService.class);

	private int port;
	private Map<String, Object> bindings;

	private GroovyShellAcceptor groovyShellAcceptor;
	private Thread acceptorThread;

	private List<String> defaultScripts = new ArrayList<String>();
	private boolean launchAtStart = true;

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
	 * Set the comma delimited list of default scripts
	 *
	 * @param scriptNames script names
	 */
	public void setDefaultScriptNames(String scriptNames) {
		defaultScripts = asList(scriptNames.split(","));
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

	private ObjectName getJMXObjectName() throws MalformedObjectNameException {
		return new ObjectName(getClass().getName() + ":port=" + port);
	}

	public void setLaunchAtStart(boolean launchAtStart) {
		this.launchAtStart = launchAtStart;
	}

	/**
	 * Opens a server socket and starts a new Thread to accept client connections.
	 *
	 * @throws IOException thrown if socket cannot be opened
	 */
	public synchronized void start() throws IOException {
		if (launchAtStart && acceptorThread == null) {
			try {
				ManagementFactory.getPlatformMBeanServer().registerMBean(this, getJMXObjectName());
			} catch (JMException e) {
				log.warn("Failed to register GroovyShellService MBean", e);
			}

			groovyShellAcceptor = new GroovyShellAcceptor(port, createBinding(bindings), defaultScripts);
			acceptorThread = new Thread(groovyShellAcceptor, "GroovyShAcceptor-" + port);
			acceptorThread.start();
		}
	}

	public synchronized void destroy() throws InterruptedException {
		if (acceptorThread != null) {
			acceptorThread.interrupt();
			acceptorThread.join();

			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(getJMXObjectName());
			} catch (JMException e) {
				log.warn("Failed to unregister GroovyShellService MBean", e);
			}

			acceptorThread = null;
		}
	}

	@Override
	public void killAllClients() {
		groovyShellAcceptor.killAllClients();
	}

	private static Binding createBinding(Map<String, Object> objects) {
		Binding binding = new Binding();

		if (objects != null) {
			for (Map.Entry<String, Object> row : objects.entrySet()) {
				binding.setVariable(row.getKey(), row.getValue());
			}
		}

		return binding;
	}
}
