package me.bazhenov.groovysh;

import jline.UnixTerminal;
import org.apache.sshd.server.Environment;

import static java.lang.Integer.parseInt;

/**
 * Overriding class for reading terminal width from SSH Environment
 */
public class SshTerminal extends UnixTerminal {

	private static final ThreadLocal<Environment> env = new ThreadLocal<Environment>();

	public SshTerminal() throws Exception {
		super();
	}

	@Override
	public int getWidth() {
		Environment environment = retrieveEnvironment();
		try {
			return parseInt(environment.getEnv().get("COLUMNS"));
		} catch (NumberFormatException e) {
			return DEFAULT_WIDTH;
		}
	}

	@Override
	public int getHeight() {
		Environment environment = retrieveEnvironment();
		try {
			return parseInt(environment.getEnv().get("LINES"));
		} catch (NumberFormatException e) {
			return DEFAULT_HEIGHT;
		}
	}

	private Environment retrieveEnvironment() {
		Environment environment = env.get();
		if (environment == null)
			throw new NullPointerException("Environement is not registered");
		return environment;
	}


	public static void registerEnvironment(Environment environment) {
		env.set(environment);
	}
}
