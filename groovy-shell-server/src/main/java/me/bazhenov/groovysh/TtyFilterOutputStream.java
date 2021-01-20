package me.bazhenov.groovysh;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

class TtyFilterOutputStream extends FilterOutputStream {

	/**
	 * Существует сценарий (как например при ручном вызове 'groovyShell.destroy()' из сессии самого GroovyShell),
	 * при котором результат команды не может быть отображен пользователю GroovyShell,
	 * поскольку ssh канал уже закрыт, что вызывает рекурсивный поток ошибок
	 * при попытке вывести пользователю уже исключение, что засоряет логи
	 * приложения эксплуатирующего GroovyShellServiceBean (см. SR-1121).
	 * <p>
	 * Поэтому, добавляем здесь явную проверку что ssh-канал для записи жив,
	 * чтобы погасить SshChannelClosedException в родительском классе и избежать такой рекурсии.
	 */
	private final AtomicBoolean isChannelAlive;

	TtyFilterOutputStream(OutputStream out, AtomicBoolean isChannelAlive) {
		super(out);
		this.isChannelAlive = isChannelAlive;
	}

	@Override
	public void write(int c) throws IOException {
		if (isChannelAlive.get()) {
			if (c == '\n') {
				super.write(c);
				c = '\r';
			}
			super.write(c);
		}
	}

	@Override
	public void flush() throws IOException {
		if (isChannelAlive.get()) {
			super.flush();
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (isChannelAlive.get()) {
			for (int i = off; i < len; i++) {
				write(b[i]);
			}
		}
	}
}
