package com.iterative.groovy.service;

import java.io.IOException;
import java.io.InputStream;

public class UtfInputStream extends InputStream {

	private final InputStream stream;

	private boolean previousByteFlag = false;
	private byte previousByte;

	public UtfInputStream(InputStream stream) {
		this.stream = stream;
	}

	@Override
	public int read() throws IOException {
		if ( previousByteFlag == true ) {
			previousByteFlag = false;
			return previousByte;
		}
		int highByte = stream.read();
		if ( highByte == -1 ) {
			return -1;
		}
		int lowByte = stream.read();

		char c = (char)((highByte << 8) | lowByte);
		byte b[] = Character.toString(c).getBytes("UTF-8");
		if ( b.length == 2 ) {
			previousByte = b[1];
			previousByteFlag = true;
			return (b[0] & 0xFF) | (b[1] & 0xFF) << 8;
		}else if ( b.length == 1 ) {
			return b[0];
		}
		throw new RuntimeException("Only 1 and 2 byte encoding supported");
	}
}
