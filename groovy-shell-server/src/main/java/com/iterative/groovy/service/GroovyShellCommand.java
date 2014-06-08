package com.iterative.groovy.service;

import groovy.lang.Binding;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

class GroovyShellCommand implements Command {

	private InputStream in;
	private OutputStream out;
	private OutputStream err;
	private ExitCallback callback;
	private Thread wrapper;

	@Override
	public void setInputStream(InputStream in) {
		this.in = in;
	}

	@Override
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}

	@Override
	public void setErrorStream(OutputStream err) {
		this.err = err;
	}

	@Override
	public void setExitCallback(ExitCallback callback) {
		this.callback = callback;
	}

	@Override
	public void start(Environment env) throws IOException {
		TtyFilterOutputStream out = new TtyFilterOutputStream(this.out);
		TtyFilterOutputStream err = new TtyFilterOutputStream(this.err);

		Binding binding = new Binding();
		binding.setVariable("out", new PrintStream(out, true, "utf8"));
		binding.setVariable("err", new PrintStream(err, true, "utf8"));

		final Groovysh shell = new Groovysh(binding, new IO(in, out, err));

		String threadName = "GroovySh Client Thread";
		wrapper = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					shell.run();
					callback.onExit(0);
				} catch (RuntimeException e) {
					callback.onExit(-1, e.getMessage());
				} catch (Error e) {
					callback.onExit(-1, e.getMessage());
				}
			}
		}, threadName);
		wrapper.start();
	}

	@Override
	public void destroy() {
		wrapper.interrupt();
	}
}
