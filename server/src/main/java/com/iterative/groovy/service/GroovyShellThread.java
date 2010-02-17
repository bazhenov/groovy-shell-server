/*
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

import java.io.*;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

/**
 * @author Bruce Fancher
 */
public class GroovyShellThread extends Thread {

	public static final String OUT_KEY = "out";
	private Socket socket;
	private Binding binding;
	private Logger log = Logger.getLogger(GroovyShellThread.class);

	public GroovyShellThread(Socket socket, Binding binding) {
		super("Groovy shell client thread: " + socket);
		this.socket = socket;
		this.binding = binding;
	}

	@Override
	public void run() {
		PrintStream out = null;
		InputStream in = null;
		try {
			out = new PrintStream(socket.getOutputStream());
			in = new UtfInputStream(socket.getInputStream());

			binding.setVariable(OUT_KEY, out);

			IO io = new IO(in, out, out);
			Groovysh shell = new Groovysh(binding, io);

			try {
				shell.run();
			} catch ( Exception e ) {
				e.printStackTrace();
			}


		} catch ( Exception e ) {
			log.error("Exception in groovy shell client thread", e);
		} finally {
			try {
				if ( out != null ) {
					out.close();
				}
				if ( in != null ) {
					in.close();
				}
				socket.close();
			} catch ( IOException e ) {
				log.error("Error while closing connection with groovy shell client", e);
			}
		}
	}

	public Socket getSocket() {
		return socket;
	}
}
