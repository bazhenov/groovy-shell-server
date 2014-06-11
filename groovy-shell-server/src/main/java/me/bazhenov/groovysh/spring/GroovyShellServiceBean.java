package me.bazhenov.groovysh.spring;

import me.bazhenov.groovysh.GroovyShellService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;

@SuppressWarnings("UnusedDeclaration")
public class GroovyShellServiceBean implements InitializingBean, DisposableBean, ApplicationContextAware {

	private final GroovyShellService service;
	private ApplicationContext applicationContext;
	private boolean launchAtStart;

	public GroovyShellServiceBean(int port) {
		this.service = new GroovyShellService();
	}

	public void setPort(int port) {
		service.setPort(port);
	}

	public void setLaunchAtStart(boolean launchAtStart) {
		this.launchAtStart = launchAtStart;
	}

	public boolean isLaunchAtStart() {
		return launchAtStart;
	}

	public void setBindings(Map<String, Object> bindings) {
		service.setBindings(bindings);
	}

	/**
	 * Set the comma delimited list of default scripts
	 *
	 * @param scriptNames script names
	 */
	public void setDefaultScriptNames(String scriptNames) {
		service.setDefaultScripts(asList(scriptNames.split(",")));
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
				Map<String, Object> bindings = service.getBindings();
				if (bindings == null)
					bindings = new HashMap<String, Object>();
				bindings.put("ctx", applicationContext);
				service.setBindings(bindings);
			}
			service.start();
		}
	}
}
