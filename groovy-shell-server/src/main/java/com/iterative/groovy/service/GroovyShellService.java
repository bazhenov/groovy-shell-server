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

import static org.slf4j.LoggerFactory.getLogger;
import groovy.lang.Binding;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;

/**
 * Instantiate this class and call {@link #start()} to create a GroovyShell server socket
 * which can accept client connections and initiate groovysh sessions.
 * 
 * <p/>Each instance of this class will register itself with the default MBean server
 * to allow for management of {@link GroovyShellServiceMBean} methods via a JMX agent.
 * 
 * @author Bruce Fancher
 * @author Denis Bazhenov
 */
public class GroovyShellService implements GroovyShellServiceMBean {

	private static final Logger log = getLogger(GroovyShellService.class);

	protected int port;
	protected Map<String, Object> bindings;

	protected GroovyShellAcceptor groovyShellAcceptor;
	protected Thread acceptorThread;

	protected List<String> defaultScripts = new ArrayList<String>();
	
	/**
	 * Uses a default port of 6789
	 */
	public GroovyShellService() {
		this(6789);
	}

	public GroovyShellService(int port) {
		this.port = port;
	}
	
	protected ObjectName getJMXObjectName() throws MalformedObjectNameException {
		return new ObjectName(getClass().getName() + ":port=" + port);
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

	/**
	 * Opens a server socket and starts a new Thread to accept client connections.
	 * 
	 * @throws IOException thrown if socket cannot be opened
	 */
	public void start() throws IOException {
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, getJMXObjectName());
		}
		catch (JMException e) {
			log.warn("Failed to register GroovyShellService MBean", e);
		}

		groovyShellAcceptor = new GroovyShellAcceptor(port, createBinding(bindings), defaultScripts);
		acceptorThread = new Thread(groovyShellAcceptor, "GroovyShAcceptor-" + port);
		acceptorThread.start();
	}

	public void destroy() throws InterruptedException {
		if (acceptorThread != null) {
			acceptorThread.interrupt();
			acceptorThread.join();

			try {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(getJMXObjectName());
			}
			catch (JMException e) {
				log.warn("Failed to unregister GroovyShellService MBean", e);
			}

			acceptorThread = null;
		}
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

	@Override
	public void killAllClients() {
		groovyShellAcceptor.killAllClients();
	}
}
