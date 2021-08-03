package me.bazhenov.groovysh.spring;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.Map;
import me.bazhenov.groovysh.GroovyShellService;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@SuppressWarnings("UnusedDeclaration")
public class GroovyShellServiceBean implements InitializingBean, DisposableBean,
    ApplicationContextAware {

  private final GroovyShellService service;
  private ApplicationContext applicationContext;
  private boolean launchAtStart = true;
  private boolean publishContextBeans = true;

  public GroovyShellServiceBean() {
    this.service = newService();
  }

  private GroovyShellService newService() {
    return new GroovyShellService();
  }

  public void setPort(int port) {
    service.setPort(port);
  }

  /**
   * @see GroovyShellService#setDisableImportCompletions(boolean)
   */
  public void setDisableImportCompletions(boolean disableImportCompletions) {
    service.setDisableImportCompletions(disableImportCompletions);
  }

  public void setLaunchAtStart(boolean launchAtStart) {
    this.launchAtStart = launchAtStart;
  }

  /**
   * @param publishContextBeans should spring beans be published in groovysh context
   */
  public void setPublishContextBeans(boolean publishContextBeans) {
    this.publishContextBeans = publishContextBeans;
  }

  public boolean isLaunchAtStart() {
    return launchAtStart;
  }

  public void setBindings(Map<String, Object> bindings) {
    service.setBindings(bindings);
  }

  protected GroovyShellService getService() {
    return service;
  }

  public void setPasswordAuthenticator(PasswordAuthenticator passwordAuthenticator) {
    service.setPasswordAuthenticator(passwordAuthenticator);
  }

  /**
   * Set the comma delimited list of default scripts
   *
   * @param scriptNames script names
   */
  public void setDefaultScriptNames(String scriptNames) {
    if (!scriptNames.trim().isEmpty()) {
      service.setDefaultScripts(asList(scriptNames.split(",")));
    }
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void destroy() throws Exception {
    service.destroy();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (launchAtStart) {
      if (applicationContext != null) {
        Map<String, Object> bindings = new HashMap<>();
        if (publishContextBeans) {
          publishContextBeans(bindings, applicationContext);
        }
        bindings.put("ctx", applicationContext);
        if (service.getBindings() != null) {
          bindings.putAll(service.getBindings());
        }
        service.setBindings(bindings);
      }
      service.start();
    }
  }

  private static void publishContextBeans(Map<String, Object> bindings, ApplicationContext ctx) {
    for (String name : ctx.getBeanDefinitionNames()) {
      if (!name.contains("#")) { // skip beans without explicit id given
        try {
          bindings.put(name, ctx.getBean(name));
        } catch (BeanIsAbstractException exc) {
          // Skip abstract beans
        }
      }
    }
  }
}
