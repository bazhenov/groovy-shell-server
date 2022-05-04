/*
 * Copyright 2007 Bruce Fancher
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package me.bazhenov.groovysh;

import org.apache.groovy.groovysh.Groovysh;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuthFactory;
import org.apache.sshd.server.auth.UserAuthNoneFactory;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;
import org.codehaus.groovy.tools.shell.util.Preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static jline.TerminalFactory.Flavor.UNIX;
import static jline.TerminalFactory.registerFlavor;
import static org.apache.groovy.groovysh.util.PackageHelper.IMPORT_COMPLETION_PREFERENCE_KEY;
import static org.apache.sshd.common.PropertyResolverUtils.updateProperty;
import static org.apache.sshd.core.CoreModuleProperties.IDLE_TIMEOUT;
import static org.apache.sshd.core.CoreModuleProperties.NIO2_READ_TIMEOUT;
import static org.apache.sshd.server.SshServer.setUpDefaultServer;

/**
 * Instantiate this class and call {@link #start()} to start a GroovyShell
 *
 * @author Denis Bazhenov
 */
@SuppressWarnings("UnusedDeclaration")
public class GroovyShellService {

  private int port;
  private String host;
  private Map<String, Object> bindings;
  private PasswordAuthenticator passwordAuthenticator;
  private long idleTimeOut = HOURS.toMillis(1);

  static final Session.AttributeKey<Groovysh> SHELL_KEY = new Session.AttributeKey<>();
  private List<String> defaultScripts = new ArrayList<>();
  private SshServer sshd;
  private boolean disableImportCompletions = false;
  private final AtomicBoolean isServiceAlive = new AtomicBoolean(true);

  /**
   * Uses a default port of 6789
   */
  public GroovyShellService() {
    this(6789);
  }

  @SuppressWarnings("WeakerAccess")
  public GroovyShellService(int port) {
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("Wrong port number");
    }
    this.port = port;
    registerFlavor(UNIX, SshTerminal.class);
  }

  public Map<String, Object> getBindings() {
    return bindings;
  }

  public void setBindings(Map<String, Object> bindings) {
    this.bindings = bindings;
  }

  public int getPort() {
    return port;
  }

  /**
   * Adds a groovy script to be executed for each new client session.
   *
   * @param script script
   */
  public void addDefaultScript(String script) {
    defaultScripts.add(script);
  }

  /**
   * @return complete List of scripts to be executed for each new client session
   */
  public List<String> getDefaultScripts() {
    return defaultScripts;
  }

  public void setPort(final int port) {
    this.port = port;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public void setIdleTimeOut(long timeOut) {
    if (timeOut < 0) {
      throw new IllegalArgumentException("Wrong timeout");
    }
    this.idleTimeOut = timeOut;
  }

  /**
   * Disable import completion autoscan.
   * <p>
   * Import completion autoscan known to cause problems on a Spring Boot applications packaged in
   * uber-jar. Please, keep in mind that value is written (and persisted) using Java Preferences
   * API. So once written it should be removed by hand (you can use groovysh <code>:set</code>
   * command).
   *
   * @see <a href="https://github.com/bazhenov/groovy-shell-server/issues/26">groovy-shell-server
   * does not work with jdk11</a>
   */
  public void setDisableImportCompletions(boolean disableImportCompletions) {
    this.disableImportCompletions = disableImportCompletions;
  }

  public void setPasswordAuthenticator(PasswordAuthenticator passwordAuthenticator) {
    this.passwordAuthenticator = passwordAuthenticator;
  }

  public void setDefaultScripts(List<String> defaultScriptNames) {
    this.defaultScripts = defaultScriptNames;
  }

  /**
   * Starts Groovysh
   *
   * @throws IOException thrown if socket cannot be opened
   */
  public synchronized void start() throws IOException {
    sshd = buildSshServer();
    sshd.start();
    if (disableImportCompletions) {
      Preferences.put(IMPORT_COMPLETION_PREFERENCE_KEY, "true");
    }
  }

  private SshServer buildSshServer() {
    SshServer sshd = setUpDefaultServer();
    sshd.setPort(port);
    if (host != null) {
      sshd.setHost(host);
    }

    long idleTimeOut = this.idleTimeOut;
    updateProperty(sshd, IDLE_TIMEOUT.getName(), idleTimeOut);
    updateProperty(sshd, NIO2_READ_TIMEOUT.getName(), idleTimeOut + SECONDS.toMillis(15L));

    sshd.addSessionListener(new SessionListener() {
      @Override
      public void sessionCreated(Session session) {
      }

      @Override
      public void sessionEvent(Session sesssion, Event event) {
      }

      @Override
      public void sessionException(Session session, Throwable t) {
      }

      @Override
      public void sessionClosed(Session session) {
        Groovysh shell = session.getAttribute(SHELL_KEY);
        if (shell != null) {
          shell.getRunner().setRunning(false);
        }
      }
    });

    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("host.key").toPath()));
    configureAuthentication(sshd);
    sshd.setShellFactory(new GroovyShellFactory());
    return sshd;
  }

  private void configureAuthentication(SshServer sshd) {
    UserAuthFactory auth;
    if (this.passwordAuthenticator != null) {
      sshd.setPasswordAuthenticator(this.passwordAuthenticator);
      auth = new UserAuthPasswordFactory();
    } else {
      auth = new UserAuthNoneFactory();
    }
    sshd.setUserAuthFactories(singletonList(auth));
  }

  public synchronized void destroy() throws IOException {
    isServiceAlive.set(false);
    sshd.stop(true);
  }

  class GroovyShellFactory implements ShellFactory {

    @Override
    public Command createShell(ChannelSession channel) {
      return new GroovyShellCommand(sshd, bindings, defaultScripts, isServiceAlive);
    }
  }
}
