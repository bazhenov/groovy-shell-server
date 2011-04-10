package com.farpost.groovy.shell;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;

import jline.ConsoleReader;

public class GroovyShellClient {

	public static void main(String[] args) throws InterruptedException, IOException {
		if ( args.length != 2 ) {
			usage();
			return;
		}
		String host = args[0];
		int port = 0;
		try {
			port = Integer.parseInt(args[1]);
		} catch ( NumberFormatException e ) {
			System.out.println("Invalid port given: " + args[1]);
		}

		SocketChannel channel;
		channel = SocketChannel.open(new InetSocketAddress(host, port));

		Thread thread = new Thread(new ReadTask(channel));
		thread.setDaemon(true);

		thread.start();

		try {
			ConsoleReader in = new ConsoleReader();
			PrintStream out = System.out;

			int i;
			ByteBuffer buffer = ByteBuffer.allocate(2);
			while ( (i = in.readVirtualKey()) >= 0 ) {
				//System.out.print("["+i+"|"+(i&0xff)+"]");
				buffer.clear();
				buffer.putChar((char) i);
				buffer.flip();
				channel.write(buffer);
			}
			out.println();
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			channel.close();
			thread.interrupt();
			thread.join();
		}
	}

	private static void usage() {
		System.out.println("Usage: remote-groovysh host port");
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
				while ( buffer.hasRemaining() ) {
					byte first = buffer.get();
					if ( (first & 0xE0) == 0xC0 ) {
						byte second = buffer.get();
						System.out.print(new String(new byte[] {first, second}, "UTF-8"));
					}else{
						System.out.print((char) first);
					}
				}
				buffer.clear();
			}
			channel.close();
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}