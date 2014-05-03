package com.iterative.groovy.service.spring;

import com.iterative.groovy.service.GroovyShellService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnusedDeclaration")
public class GroovyShellServiceBean implements InitializingBean, DisposableBean, ApplicationContextAware {

	private final GroovyShellService service;
	private ApplicationContext applicationContext;

	public GroovyShellServiceBean(int port) {
		this.service = new GroovyShellService();
	}

	public void setPort(int port) {
		service.setPort(port);
	}

	public void setLaunchAtStart(boolean launchAtStart) {
		service.setLaunchAtStart(launchAtStart);
	}

	public void setBindings(Map<String, Object> bindings) {
		service.setBindings(bindings);
	}

	public void setDefaultScriptNames(String scriptNames) {
		service.setDefaultScriptNames(scriptNames);
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
		if (applicationContext != null) {
			Map<String, Object> bindings = service.getBindings();
			if (bindings == null)
				bindings = new HashMap<>();
			bindings.put("ctx", applicationContext);
			service.setBindings(bindings);
		}
		service.start();
	}
}
