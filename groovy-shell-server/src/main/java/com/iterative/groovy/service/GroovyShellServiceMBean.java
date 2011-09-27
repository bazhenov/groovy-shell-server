package com.iterative.groovy.service;

/**
 * Interface containing GroovyShellService methods to be exposed via JMX.
 */
public interface GroovyShellServiceMBean {
	
	/**
	 * Forcibly kill all client threads associated with this GroovyShellService.  An interrupt will be sent
	 * to each thread, after which the client socket will be closed.  If the thread doesn't exit normally at that
	 * point, it will be forcibly stopped via {@link Thread#stop()}.
	 * 
	 * <p/>There are known <a href="http://download.oracle.com/javase/1.4.2/docs/guide/misc/threadPrimitiveDeprecation.html">limitations and risks<a/>
	 * associated with killing Threads, so keep in mind this is primarily intended for use in development, or as an "emergency brake" if some clients go
	 * haywire on a production system and the risk of inconsistent object state is outweighed by the need to stop the client thread from continuing.
	 */
	void killAllClients();

}
