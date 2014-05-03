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
		<version>1.3</version>
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

Groovy shell server use `socat` as a client. So connecting to a groovy shell server as simple as:

	$ socat -,raw,echo=0,opost TCP:127.0.0.1:6789
	Groovy Shell (2.1.9, JVM: 1.6.0_65)
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

You can make alias to simplify your life:

	$ alias groovy-shell='socat -,raw,echo=0,opost'
	$ groovy-shell TCP:127.0.0.1:6789
	Groovy Shell (2.1.9, JVM: 1.6.0_65)
	Type 'help' or '\h' for help.
	-------------------------------------------------------------------------------
	groovy:000>

Integrating with Spring
-----------------------
You can easily integrate Groovy Shell with Spring container:

	<bean class="com.iterative.groovy.service.spring.GroovyShellServiceBean"
		p:port="6789"
		p:lauchAtStart="true"
		p:bindings-ref="bindings"/>

	<u:map id="bindings">
		<entry key="foo" value="bar"/>
	</u:map>

When using `GroovyShellServiceBean` reference to the `ApplicationContext` is added to bindings implicitly, so in shell you can get objects
from container by id or type (e.g. `ctx.getBean('id')`).

Build
-----
Use `maven-assembly` plugin to build and create archive of `groovy-shell-server`:

	mvn package

Archives will be placed in `groovy-shell-server/target/`.

### Simple run

In order to simple run applications you can use `maven-exec` plugin:

	mvn -f groovy-shell-server/pom.xml exec:java -Dexec.mainClass=com.iterative.groovy.service.Main

Management
----------

What if a well-meaning developer fires up a remote shell and accidentally executes a script which hammers the server?	Fortunately,
each GroovyShellService instance registers itself with the default MBeanServer and provides a "killAllClients" operation to kill
any open client sockets and stop the associated client threads. Thus you can connect with jconsole or your favorite JMX frontend
to resolve this issue if it arises.
