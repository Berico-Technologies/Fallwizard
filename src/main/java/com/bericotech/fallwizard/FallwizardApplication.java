package com.bericotech.fallwizard;

import com.bericotech.fallwizard.auth.SpringSecurityAuthProvider;
import com.bericotech.fallwizard.configuration.FallwizardConfiguration;
import com.bericotech.fallwizard.configuration.SpringConfiguration;
import com.bericotech.fallwizard.configuration.SpringPropertyPlaceholderConfigurerConfiguration;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Rename the service to be FallwizardService.
 * This is using the FallwizardConfiguration (which specifies from the DropWizard configuration
 * file which spring components to inject).
 *
 * @author Richard Clayton
 * @author Justin McCune  (update to 1.2)
 * Date: 12/14/13
 */
public class FallwizardApplication<T extends FallwizardConfiguration> extends Application<T> {

    private static final Logger logger = LoggerFactory.getLogger(FallwizardApplication.class);

    // If a prefix is missing on the location of an Application Context file
    // the default resource type will be a file.
    public static String DEFAULT_RESOURCE_TYPE = "file";

    // Instantiate the Spring Application Context
    protected GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();

    @Override
    public void initialize(Bootstrap<T> bootstrap) {}

    @Override
    public void run(T configuration, Environment environment) throws Exception {

        applicationContext.getBeanFactory().registerResolvableDependency(MetricRegistry.class, environment.metrics());
        logger.info("Starting up FallWizardService");

        // Populate the applicationContext based on the Spring Configuration
        initSpringConfig(configuration.getSpringConfiguration(),environment);

        // Stand up all the DropWizard Objects (from the Spring context files).
        registerManaged(environment);
        registerLifecycle(environment);
        registerTasks(environment);
        registerHealthChecks(environment);
        registerProviders(environment);
        registerResources(environment);
        registerContextAsManaged(environment);
    }

    private void registerContextAsManaged(Environment environment) {
        environment.lifecycle().manage (new Managed() {
            @Override
            public void start() throws Exception {}

            @Override
            public void stop() throws Exception {
                applicationContext.close(); // close the door when you leave
            }
        });
    }


    private void initSpringConfig(SpringConfiguration springConfiguration, Environment environment) {


        logger.info("Using configurations: {}", join(springConfiguration.getApplicationContext(), ", "));


        SpringPropertyPlaceholderConfigurerConfiguration propConfig =
                springConfiguration.getPlaceholderConfiguration();

        //If the YAML File defines property locations, or properties directly for us to inject
        // into the spring context files we are loading, we create the PropertyPlaceholderConfigurer
        if (propConfig!=null) {
            PropertyPlaceholderConfigurer configurer = propConfig.createPlaceholderConfigurer(applicationContext);
            applicationContext.getBeanFactory().registerSingleton("fallWizardProperties", configurer);
        }

        // ------------------------------
        // Load Profiles
        // ------------------------------
        // If Spring Bean Profiles are defined, register the profiles.
        if (springConfiguration.getBeanProfiles() != null){
            logger.info("Using profiles: {}", join(springConfiguration.getBeanProfiles(), ", "));
            applicationContext.getEnvironment()
                    .setActiveProfiles(springConfiguration.getBeanProfiles());
        }

        // ------------------------------
        // Load Application Context Files
        // ------------------------------
        for (String applicationContextFile : springConfiguration.getApplicationContext()){

            applicationContext.load(
                    normalizeForResourceLocation(applicationContextFile));
        }

        applicationContext.refresh();

        // ------------------------------
        // SpringSecurity Considerations
        // ------------------------------
        // If we should use Spring Security
        if (springConfiguration.shouldUseSpringSecurity()){
            final XmlWebApplicationContext wctx = new XmlWebApplicationContext();
            wctx.setParent(applicationContext);
            wctx.setConfigLocation("");
            wctx.refresh();
            environment.servlets().addServletListeners(new ServletContextListener() {

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
            environment.jersey().register(resource);
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
            environment.lifecycle().manage(managed);

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

            environment.lifecycle().manage(lifeCycle);

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
            environment.admin().addTask(task);

            logger.info("Registering task: " + task.getClass().getName());
        }
    }

    /**
     * Register Dropwizard Healthchecks.
     * @param environment Dropwizard Environment
     */
    private void registerHealthChecks(Environment environment) {

        final Map<String, HealthCheck> beansOfType = applicationContext.getBeansOfType(HealthCheck.class);

        for (Map.Entry<String, HealthCheck> entry : beansOfType.entrySet()) {
            environment.healthChecks().register(entry.getKey(),entry.getValue());
            logger.info("Registering healthCheck: " + entry.getValue().getClass().getName());
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

            //environment.addProvider(provider);
            //logger.info("Registering provider : " + provider.getClass().getName());
        }
    }
}
