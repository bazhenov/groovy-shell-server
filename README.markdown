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
		<version>2.2.1</version>
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

As of 1.5 Groovy shell server use plain `ssh` as a client. So connecting to a groovy shell server as simple as:

	$ ssh 127.1 -p 6789
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

By default, no authentication is required (any username is allowed to open a SSH connection). You can enable password authentication by creating your own implementation of `org.apache.sshd.server.PasswordAuthenticator` interface and passing an instance to server:
	
	PasswordAuthenticator myPasswordAuthenticator = new MyPasswordAuthenticator();
	service.setPasswordAuthenticator(myPasswordAuthenticator);

Integrating with Spring
-----------------------
You can easily integrate Groovy Shell with Spring container:

	<bean class="me.bazhenov.groovysh.spring.GroovyShellServiceBean"
		p:port="6789"
		p:launchAtStart="true"
		p:publishContextBeans="true"
		p:bindings-ref="bindings"/>

	<u:map id="bindings">
		<entry key="foo" value="bar"/>
	</u:map>

When `publishContextBeans` is true all context beans are published to groovy shell context. So bean with id `foo`
will be available as `foo` in groovy shell. Also reference to the `ApplicationContext` is added to bindings implicitly
as `ctx`. So in shell you can get objects from container by id or type (e.g. `ctx.getBean('id')`).

It is also possible to enable password authentication by setting `passwordAuthenticator` property on `GroovyShellServiceBean`.

### Simple run

In order to simple run applications you can use `maven-exec` plugin:

	mvn -f groovy-shell-server/pom.xml exec:java -Dexec.mainClass=me.bazhenov.groovysh.Main

Management
----------

What if a well-meaning developer fires up a remote shell and accidentally executes a script which hammers the server?	Fortunately,
each GroovyShellService instance registers itself with the default MBeanServer and provides a "killAllClients" operation to kill
any open client sockets and stop the associated client threads. Thus you can connect with jconsole or your favorite JMX frontend
to resolve this issue if it arises.