Groovysh Server
===============

Introduction
------------

If you are familiar with groovy, you know what `groovysh` is. It's damn simple REPL (read, evaluate, print, loop) shell for evaluating
groovy code. And `groovy-shell-server` is full featured groovy shell inside your application.

How many times you are in situation when all you need is to call some method inside your application, but the only way to do it
is JMX or custom user interface (web page, for instance)? Groovy shell server allows you to run REPL shell inside your application
and work with it like you are using `groovysh`.

Groovy shell server uses `groovysh` API inside, so all features of `groovysh` (autocompletion, history etc.) are supported.

Installation
------------

Just include following dependency in your `pom.xml`:

	<dependency>
		<groupId>me.bazhenov.groovy-shell</groupId>
		<artifactId>groovy-shell-server</artifactId>
		<version>1.2</version>
	</dependency>

Using
-----

In your application you should start `GroovyShellService`:

	GroovyShellService service = new GroovyShellService();
	service.setPort(6789);
	service.setBindings(new HashMap<String, Object>() {{
		put("foo", obj1);
		put("bar", obj2);
	}});
								
	service.start();

And destroy it on application exit:

	service.destroy();

Groovy shell server use custom client for connect to a server. After you download a package, you should unpack it
and it's ready to go:

	$ tar xvf groovy-shell-client-1.0.tar.gz 
	x groovy-shell-client-1.0/lib/slf4j-api-1.6.1.jar
	x groovy-shell-client-1.0/lib/jline-0.9.94.jar
	x groovy-shell-client-1.0/lib/logback-classic-0.9.25.jar
	x groovy-shell-client-1.0/lib/logback-core-0.9.25.jar
	x groovy-shell-client-1.0/lib/groovy-shell-client-1.0.jar
	x groovy-shell-client-1.0/bin/remote-groovysh
	$ ./groovy-shell-client-1.0/bin/remote-groovysh localhost 6789
	Groovy Shell (1.5.6, JVM: 19.1-b02-334)
	Type 'help' or '\h' for help.
	-------------------------------------------------------------------------------
	groovy:000> (1..10).each { println "Kill all humans!" }
	Kill all humans!
	Kill all humans!
	Kill all humans!
	Kill all humans!
	Kill all humans!
	Kill all humans!
	Kill all humans!
	Kill all humans!
	Kill all humans!
	Kill all humans!
	===> 1..10
	groovy:000>

Build
-----
Use `maven-assembly` plugin to build and create archive of `groovy-shell-server`:

	mvn -f groovy-shell-server/pom.xml assembly:assembly

and `groovy-shell-client`:

	mvn -f groovy-shell-client/pom.xml assembly:assembly

Archives will be placed in `groovy-shell-server/target/` and `groovy-shell-client/target/` respectively.

### Simple run

In order to simple run applications you can use `maven-exec` plugin:

	mvn -f groovy-shell-server/pom.xml exec:java -Dexec.mainClass=com.iterative.groovy.service.Main

and

	mvn -f groovy-shell-client/pom.xml exec:java -Dexec.mainClass=com.farpost.groovy.shell.GroovyShellClient -Dexec.args="localhost 6789"

Management
----------

What if a well-meaning developer fires up a remote shell and accidentally executes a script which hammers the server?  Fortunately, 
each GroovyShellService instance registers itself with the default MBeanServer and provides a "killAllClients" operation to kill
any open client sockets and stop the associated client threads.  Thus you can connect with jconsole or your favorite JMX frontend
to resolve this issue if it arises.
