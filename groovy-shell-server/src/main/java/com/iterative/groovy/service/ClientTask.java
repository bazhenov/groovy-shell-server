package com.iterative.groovy.service;

import groovy.lang.Binding;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;

import static org.slf4j.LoggerFactory.getLogger;

public class ClientTask implements Runnable {

	private final Socket socket;
	private final Binding binding;
	private final static Logger log = getLogger(ClientTask.class);

	public ClientTask(Socket socket, Binding binding) {
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

			binding.setVariable("out", out);

			IO io = new IO(in, out, out);
			Groovysh shell = new Groovysh(binding, io);

			try {
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
