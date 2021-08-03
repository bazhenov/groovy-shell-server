package me.bazhenov.groovysh.thread;

import org.apache.sshd.server.session.ServerSession;

public interface ServerSessionAwareThreadFactory {

  Thread newThread(Runnable runnable, ServerSession session);

}
