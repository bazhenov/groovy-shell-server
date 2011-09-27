package com.iterative.groovy.service;

import org.slf4j.Logger;

import static java.lang.Thread.currentThread;
import static org.slf4j.LoggerFactory.getLogger;

public class ClientTaskThread extends Thread {

	private static final Logger log = getLogger(ClientTaskThread.class);

	private ClientTask clientTask;

	public ClientTaskThread(ClientTask runnable, String threadName) {
		super(runnable, threadName);
		this.clientTask = runnable;
	}

	@SuppressWarnings("deprecation")
	public void kill() {
		interrupt();
		clientTask.closeSocket();
		try {
			join(500);
		} catch (InterruptedException e) {
			// Restore interrupt thread flag
			currentThread().interrupt();
		}

		if (isAlive()) {
			log.warn(getName() + " not responding to interrupts ... forcibly stopping");
			stop();
		}
	}

}
