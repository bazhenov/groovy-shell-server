/**
 * Copyright 2011 Denis Bazhenov
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
import groovy.lang.Closure;
import org.codehaus.groovy.tools.shell.Command;
import org.codehaus.groovy.tools.shell.ExitNotification;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.List;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Denis Bazhenov
 */
public class ClientTask implements Runnable {

	private final static Logger log = getLogger(ClientTask.class);

	private final Socket socket;
	private final Binding binding;
	private final List<String> defaultScripts;

	public ClientTask(Socket socket, Binding binding, List<String> defaultScripts) {
		this.socket = socket;
		this.binding = binding;
		this.defaultScripts = defaultScripts;
	}

	@Override
	@SuppressWarnings({"unchecked", "serial"})
	public void run() {
		PrintStream out = null;
		InputStream in = null;
		try {
			out = new PrintStream(socket.getOutputStream(), true, "utf8");
			in = socket.getInputStream();

			binding.setVariable("out", out);

			IO io = new IO(in, out, out);
			Groovysh shell = new Groovysh(binding, io);

			Exception cause = null;
			try {
				loadDefaultScripts(shell);
			} catch (Exception e) {
				log.error("Error while loading default script", e);
				cause = e;
			}

			final Closure<Groovysh> defaultErrorHook = shell.getErrorHook();
			shell.setErrorHook(new Closure<Groovysh>(this) {
				@Override
				public Groovysh call(Object... args) {
					// If we see that the socket is closed, we ask the REPL loop to exit immediately
					if (socket.isClosed()) {
						throw new ExitNotification(0);
					}
					return defaultErrorHook.call(args);
				}
			});

			try {
				if (cause != null) {
					out.println("Unable to load default script: " + cause);
				}
				shell.run();
			} catch (Exception e) {
				log.error("Error while executing client command", e);
			}

		} catch (Exception e) {
			log.error("Exception in groovy shell client thread", e);

		} finally {
			closeQuietly(in);
			closeQuietly(out);
			closeQuietly(socket);
		}
	}

	@SuppressWarnings({"unchecked", "serial"})
	private void loadDefaultScripts(final Groovysh shell) {
		if (!defaultScripts.isEmpty()) {
			Closure<Groovysh> defaultResultHook = shell.getResultHook();

			try {
				// Set a "no-op closure so we don't get per-line value output when evaluating the default script
				shell.setResultHook(new Closure<Groovysh>(this) {
					@Override
					public Groovysh call(Object... args) {
						return shell;
					}
				});

				Command cmd = shell.getRegistry().find("load");
				for (String script : defaultScripts) {
					cmd.execute(asList(script));
				}
			} finally {
				// Restoring original result hook
				shell.setResultHook(defaultResultHook);
			}
		}
	}

	public void closeSocket() {
		closeQuietly(socket);
	}

	private static void closeQuietly(Closeable object) {
		try {
			object.close();
		} catch (IOException e) {
			log.warn("Error while closing object", e);
		}
	}

	private static void closeQuietly(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			log.warn("Error while closing socket", e);
		}
	}
}