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
package com.farpost.groovy.shell;

import jline.ConsoleReader;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import static java.lang.Thread.currentThread;

 /**
  * Client which can connect to a server socket provided by com.iterative.groovy.service.GroovyShellService.
  * To use from the command line, provide <code>host</code> and <code>port</code> as arguments.  For example:
  * 
  * <pre>
  * java -cp "groovy-shell-client-1.1.jar;jline-0.9.94.jar" com.farpost.groovy.shell.GroovyShellClient localhost 6789
  * </pre>
  * 
  * @author Denis Bazhenov
  *
  */
public class GroovyShellClient {

	protected String host;
	protected int port;
	
	public GroovyShellClient(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	/**
	 * Runs the client, exiting when either the client input stream or server output stream is closed.
	 */
	public void run() throws IOException, InterruptedException {
		SocketChannel channel = SocketChannel.open(new InetSocketAddress(host, port));

		Thread socketThread = new Thread(new ReadSocketTask(channel));
		socketThread.start();

		Thread consoleThread = new Thread(new ReadConsoleTask(channel, System.out));
		consoleThread.setDaemon(true);
		consoleThread.start();

		socketThread.join();
		channel.close();
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		if (args.length != 2) {
			System.out.println("Usage: java com.farpost.groovy.shell.GroovyShellClient host port");
			System.exit(1);
		}
		String host = args[0];
		int port = 0;
		try {
			port = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			System.out.println("Invalid port given: " + args[1]);
			System.exit(1);
		}
		new GroovyShellClient(host, port).run();
	}
}

/**
 * @author Denis Bazhenov
 */
class ReadSocketTask implements Runnable {

	private final SocketChannel channel;

	public ReadSocketTask(SocketChannel channel) {
		this.channel = channel;
	}

	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		try {
			while (channel.read(buffer) != -1 && !currentThread().isInterrupted()) {
				buffer.flip();
				while (buffer.hasRemaining()) {
					byte first = buffer.get();
					if ((first & 0xE0) == 0xC0) {
						byte second = buffer.get();
						System.out.print(new String(new byte[]{first, second}, "UTF-8"));
					} else {
						System.out.print((char) first);
					}
				}
				buffer.clear();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

class ReadConsoleTask implements Runnable {

	private final WritableByteChannel channel;
	private final PrintStream out;

	public ReadConsoleTask(WritableByteChannel channel, PrintStream out) {
		this.channel = channel;
		this.out = out;
	}

	@Override
	public void run() {
		try {
			ConsoleReader in = new ConsoleReader();

			int i;
			ByteBuffer buffer = ByteBuffer.allocate(2);
			while ((i = in.readVirtualKey()) >= 0 && channel.isOpen()) {
				buffer.clear();
				buffer.putChar((char) i);
				buffer.flip();
				channel.write(buffer);
			}
			out.println();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}