package me.bazhenov.groovysh.thread;

import org.apache.sshd.server.session.ServerSession;

public class DefaultGroovyshThreadFactory implements ServerSessionAwareThreadFactory {

    @Override
    public Thread newThread(Runnable runnable, ServerSession session) {
        String threadName = "GroovySh Client Thread: " + session.getIoSession().getRemoteAddress().toString();
        return new Thread(runnable, threadName);
    }
}
