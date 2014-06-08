package com.iterative.groovy.service;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class TtyFilterOutputStream extends FilterOutputStream {

	public TtyFilterOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(int c) throws IOException {
		if (c == '\n') {
			super.write(c);
			c = '\r';
		}
		super.write(c);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		for (int i = off; i < len; i++) {
			write(b[i]);
		}
	}
}
