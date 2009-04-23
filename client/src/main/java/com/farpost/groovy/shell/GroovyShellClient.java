package com.farpost.groovy.shell;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;

import jline.ConsoleReader;

public class GroovyShellClient {

	public static void main(String[] args) throws InterruptedException, IOException {
		SocketChannel channel = null;
		channel = SocketChannel.open(new InetSocketAddress("localhost", 6789));

		Thread thread = new Thread(new ReadTask(channel));
		thread.setDaemon(true);

		thread.start();

		try {
			ConsoleReader in = new ConsoleReader();
			PrintStream out = System.out;

			int i;
			ByteBuffer buffer = ByteBuffer.allocate(1);
			while ( (i = in.readVirtualKey()) >= 0 ) {
				buffer.clear();
				buffer.put((byte) i);
				buffer.flip();
				channel.write(buffer);
			}
			out.println();
		} catch ( IOException e ) {
		} finally {
			channel.close();
			thread.interrupt();
			thread.join();
		}
	}
}

class ReadTask implements Runnable {

	private final SocketChannel channel;

	public ReadTask(SocketChannel channel) {
		this.channel = channel;
	}

	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		try {
			while ( channel.read(buffer) != 0 ) {
				if ( Thread.currentThread().isInterrupted() ) {
					return;
				}
				buffer.flip();
				for ( int i = buffer.position(); i < buffer.limit(); i++ ) {
					byte c = buffer.get(i);
					System.out.print((char) c);
				}
				buffer.clear();
			}
			channel.close();
		} catch ( IOException e ) {
		}
	}
}