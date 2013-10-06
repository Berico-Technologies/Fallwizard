package com.berico.fallwizard.auth;

import javax.servlet.Filter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.server.session.SessionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.dropwizard.auth.Auth;
import com.yammer.dropwizard.config.Environment;

/**
 * This is used to inject the UserDetails object into REST
 * endpoint functions when the @Auth annotation is used.
 * 
 * @author Richard Clayton (Berico Technologies)
 */
public class SpringSecurityAuthProvider implements InjectableProvider<Auth, Parameter> {

	
	ApplicationContext applicationContext;
	
	public SpringSecurityAuthProvider(ApplicationContext applicationContext){
		
		this.applicationContext = applicationContext;
	}
	
	public void registerProvider(Environment environment){
		
		environment.setSessionHandler(new SessionHandler());
		
		registerSpringSecurityFilters(environment);
		
		environment.addProvider(this);
	}
	
	protected void registerSpringSecurityFilters(Environment environment){
		DelegatingFilterProxy proxy = new DelegatingFilterProxy();
		environment.addFilter(DelegatingFilterProxy.class, "/*").setName("springSecurityFilterChain");
	}
	
	@Override
	public Injectable<UserDetails> getInjectable(ComponentContext componentContext, Auth auth, Parameter parameter) {
		
		return new SpringSecurityInjectable(auth.required());
	}
	
	@Override
	public ComponentScope getScope() {
		
		return ComponentScope.PerRequest;
	}
	
	private static class SpringSecurityInjectable extends AbstractHttpContextInjectable<UserDetails> {

		private final boolean authenticationRequired;
		
		public SpringSecurityInjectable(boolean authenticationRequired){
			
			this.authenticationRequired = authenticationRequired;
		}
		
		@Override
		public UserDetails getValue(HttpContext context) {
			
			UserDetails userDetails = null;
			
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			
			if (!(auth instanceof AnonymousAuthenticationToken)) {
			    
				userDetails =
						 (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			}
			
			if (userDetails == null && authenticationRequired){
				
				throw new WebApplicationException(
					Response
						.status(Response.Status.UNAUTHORIZED)
						.entity("Credentials are required to access this resource.")
	                    .type(MediaType.TEXT_PLAIN_TYPE)
	                    .build());
			} 
			
			if (userDetails == null ) {
					
				userDetails = new AnonymousUserDetails();
			}
			
			return userDetails;
		}
	
	}

}
