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

import java.io.IOException;
import java.util.Map;

/**
 * @author Bruce Fancher
 * @author Denis Bazhenov
 */
public class GroovyShellService {

	private int port = 6789;

	private Map<String, Object> bindings;
	private Thread acceptorThread;

	public void setBindings(Map<String, Object> bindings) {
		this.bindings = bindings;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public void start() throws IOException {
		Runnable acceptor = new GroovyShellAcceptor(port, createBinding(bindings));
		acceptorThread = new Thread(acceptor, "Groovy shell acceptor thread");
		acceptorThread.start();
	}

	public void destroy() throws InterruptedException {
		if (acceptorThread != null) {
			acceptorThread.interrupt();
			acceptorThread.join();
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
}
