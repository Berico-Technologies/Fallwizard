/* I want to recognize Nicolas Huray for his work on "dropwizard-spring", which I stole
 * a lot of ideas and some source code:  https://github.com/nhuray/dropwizard-spring
 * The Apache Software License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package com.berico.fallwizard;

import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.berico.fallwizard.auth.SpringSecurityAuthProvider;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.Managed;
import com.yammer.dropwizard.tasks.Task;
import com.yammer.metrics.core.HealthCheck;

import org.eclipse.jetty.util.component.LifeCycle;


/**
 * Service abstraction that injects Spring managed components into
 * the Dropwizard environment.
 * 
 * @author Richard Clayton (Berico Technologies)
 */
public abstract class SpringService<T extends SpringConfiguration> extends Service<T> {

	private static final Logger logger = LoggerFactory.getLogger(SpringService.class);
	
	// If a prefix is missing on the location of an Application Context file
	// the default resource type will be a file.
	public static String DEFAULT_RESOURCE_TYPE = "file";
	
	// Instantiate the Spring Application Context
	protected GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();;
	
	@Override
	public void initialize(Bootstrap<T> bootstrap) {}

	@Override
	public void run(T configuration, Environment environment) throws Exception {
		
		logger.info("Starting up SpringService");
		logger.info("Using configurations: {}", 
			join(configuration.getApplicationContext(), ", "));
		
		for (String applicationContextFile : configuration.getApplicationContext()){
			
			applicationContext.load(
					normalizeForResourceLocation(applicationContextFile));
		}
		
		// If Spring Bean Profiles are defined, register the profiles.
		if (configuration.getBeanProfiles() != null){
			
			logger.info("Using profiles: {}", join(configuration.getBeanProfiles(), ", "));

            applicationContext.getEnvironment()
				.setActiveProfiles(configuration.getBeanProfiles());
		}
		
		applicationContext.refresh();
		
		// If we should use Spring Security
		if (configuration.shouldUseSpringSecurity()){
			final XmlWebApplicationContext wctx = new XmlWebApplicationContext();
			wctx.setParent(applicationContext);
			wctx.setConfigLocation("");
			wctx.refresh();
			environment.addServletListeners(new ServletContextListener() {

				@Override
				public void contextInitialized(ServletContextEvent servCtx) {
					servCtx.getServletContext()
							.setAttribute(
									WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
									wctx);
					wctx.setServletContext(servCtx.getServletContext());

				}

				@Override
				public void contextDestroyed(ServletContextEvent arg0) {
					// TODO Auto-generated method stub

				}
			});
			// Register the Spring Security Auth Provider
			new SpringSecurityAuthProvider(applicationContext)
				.registerProvider(environment);
		}
		
		registerManaged(environment);
        registerLifecycle(environment);
        registerTasks(environment);
        registerHealthChecks(environment);
        registerProviders(environment);
        registerResources(environment);
	}

	/**
	 * Little utility to concatenate strings with a separator.
	 * @param strings Strings to join
	 * @param separator Separator between strings
	 * @return Joined String
	 */
	String join(String[] strings, String separator){
		
		StringBuilder sb = new StringBuilder();
		
		for(String string : strings){
			
			sb.append(string).append(separator);
		}
		
		return sb.delete(sb.length() - separator.length(), sb.length()).toString();
	}
	
	String normalizeForResourceLocation(String contextLocation){
		
		boolean hasResource = contextLocation.startsWith("file") 
				|| contextLocation.startsWith("classpath")
				|| contextLocation.startsWith("url");
		
		if (!hasResource){
			
			return String.format("%s:%s", DEFAULT_RESOURCE_TYPE, contextLocation);
		}
		
		return contextLocation;
	}
	
	/**
	 * Registered JAX-RS Endpoints
	 * @param environment Dropwizard Environment
	 */
	private void registerResources(Environment environment){
		
		final Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(Path.class);
		
        for (String beanName : beansWithAnnotation.keySet()) {
        	
            Object resource = beansWithAnnotation.get(beanName);
            
            environment.addResource(resource);
            
            logger.info("Registering resource : " + resource.getClass().getName());
        }
	}

	/**
	 * Register Dropwizard Managed-Lifecycle Components
	 * @param environment Dropwizard Environment
	 */
	private void registerManaged(Environment environment) {
		
        final Map<String, Managed> beansOfType = applicationContext.getBeansOfType(Managed.class);
        
        for (String beanName : beansOfType.keySet()) {
            
            Managed managed = beansOfType.get(beanName);
            
            environment.manage(managed);
            
            logger.info("Registering managed: " + managed.getClass().getName());
        }
    }
	
	/**
	 * Register Jetty Lifecycle Components.
	 * @param environment Dropwizard Environment
	 */
	private void registerLifecycle(Environment environment) {
		
        Map<String, LifeCycle> beansOfType = applicationContext.getBeansOfType(LifeCycle.class);
        
        for (String beanName : beansOfType.keySet()) {
            
        		LifeCycle lifeCycle = beansOfType.get(beanName);
                
        		environment.manage(lifeCycle);
                
        		logger.info("Registering lifeCycle: " + lifeCycle.getClass().getName());
        }
    }
	
	/**
	 * Register Dropwizard Tasks.
	 * @param environment Dropwizard Environment
	 */
	private void registerTasks(Environment environment) {
		
        final Map<String, Task> beansOfType = applicationContext.getBeansOfType(Task.class);
        
        for (String beanName : beansOfType.keySet()) {
            
            Task task = beansOfType.get(beanName);
            
            environment.addTask(task);
            
            logger.info("Registering task: " + task.getClass().getName());
        }
    }
	
	/**
	 * Register Dropwizard Healthchecks.
	 * @param environment Dropwizard Environment
	 */
	private void registerHealthChecks(Environment environment) {
		
        final Map<String, HealthCheck> beansOfType = applicationContext.getBeansOfType(HealthCheck.class);
        
        for (String beanName : beansOfType.keySet()) {
        	
            HealthCheck healthCheck = beansOfType.get(beanName);
            
            environment.addHealthCheck(healthCheck);
            
            logger.info("Registering healthCheck: " + healthCheck.getClass().getName());
        }
    }
	
	/**
	 * Register JAX-RS Injectable Providers
	 * @param environment Dropwizard Environment
	 */
    private void registerProviders(Environment environment) {
    	
        final Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(Provider.class);
        
        for (String beanName : beansWithAnnotation.keySet()) {
	        	
            Object provider = beansWithAnnotation.get(beanName);
            
            environment.addProvider(provider);
            
            logger.info("Registering provider : " + provider.getClass().getName());
        }
    }
}
