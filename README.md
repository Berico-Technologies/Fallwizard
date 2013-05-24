# Fallwizard

[![Build Status](https://travis-ci.org/tlpinney/Fallwizard.png)](https://travis-ci.org/tlpinney/Fallwizard)


Leveraging the best of the Spring Framework and Dropwizard.

__Maven Dependency__
```xml
<repositories>
  <repository>
    <id>nexus.bericotechnologies.com</id>
    <name>Berico Technologies Nexus</name>
    <url>http://nexus.bericotechnologies.com/content/groups/public</url>
    <releases><enabled>true</enabled></releases>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.berico</groupId>
    <artifactId>fallwizard</artifactId>
    <version>1.1.0</version>
  </dependency>
</dependencies>
```
__Maven Archetype (Fallwizard Quickstart)__
```bash
mvn archetype:generate  \
  -DarchetypeCatalog=http://nexus.bericotechnologies.com/content/repositories/releases/archetype-catalog.xml
```

## Why?

We love Spring Framework because it simplified Enterprise Java.  We love Dropwizard because it simplified App Servers.  What if these two frameworks made sweet-sweet love and had a child?  Well, you would get Fallwizard.

Ok seriously...

Fallwizard is the integration of Spring with Dropwizard.  This gives us the following benefits we were missing when using pure Dropwizard:

* Ability to support complex configurations outside of the yaml mechanism (i.e. we need more dynamicism).
* Better support for complex authentication and authorization environments.

> This is not a condemnation of Dropwizard.  Most organizations don't need the flexibility we need in configuration and security.

So, what's up with the name?

Spring -> Fall <- Drop...wizard.  Get it?  (Ok, it's corny.)

## Features

* Define Resources, Healthchecks, Tasks, etc. in Spring, and have them automatically bootstrapped in Dropwizard.
* Utilize Spring Security (out of the box) and have it applied to Dropwizard routes.
* Automatically inject a Spring Security UserDetails object into REST methods using Dropwizard's `@Auth` annotation.

## Changelog

#### Release 1.1.0

* Added Support for Bean Profiles.  Now you can specify a "beanProfiles" property (Array of String) in the Yaml config to specify the Bean Profiles you want to use.  This should eliminate the need of maintaining different lists of context files for production/dev/integation configurations.
* No more reliance on `FileSystemXmlApplicationContext`.  We have switched to favor `GenericXmlApplicationContext`, which will allow you to use `file`, `classpath`, and `url` resources.  __I made one opinionated change to the way Spring handle configuration by default!__  If an application context file is not "prefixed" with either `file`, `classpath`, or `url`, Fallwizard will default to `file` NOT `classpath`.  The assumption is that you will probably have your configuration in a subdirectory outside of the FAT JAR.

#### Release 1.0.0

* Spring contexts loaded from yaml specified xml files.
* Spring Security implementation of the Dropwizard Authentication plugin.

## Related

__We have a Maven Archetype for Fallwizard!__

```bash
mvn archetype:generate  \
  -DarchetypeCatalog=http://nexus.bericotechnologies.com/content/repositories/releases/archetype-catalog.xml
```

Select `fallwizard-archetype` from the list.

__One of our engineers, Travis, has developed a Gradle Plugin for building Dropwizard projects__

https://github.com/Berico-Technologies/dropwizard-gradle-plugin


## Using Fallwizard

### Configuring Fallwizard

#### Fallwizard Conventions

Instead of using Dropwizard's `Service` and `Configuration` classes, you should extend (if you want) `SpringService` and `SpringConfiguration`.  If you set your configuration values and wireup your resources in Spring, you actually don't need to extend `SpringService` (just specify this as your main class in Maven/Gradle).  

If you need to add Dropwizard bundles to your service, you will need to extend `SpringService`; please remember to call `super` on any method you override.

#### Yaml Configuration

```yaml
# Application Contexts to Load.
applicationContext: ['conf/applicationContext.xml', 'file:conf/basicAuthSecurityContext.xml', 
     'classpath:sprocketContext.xml', 'url:http://conf.berico.us/sproket-manager/context.xml']

# Spring Bean Profiles to use.
beanProfiles: ['production', 'feature1', 'feature2']

# Should Spring Security be used?
useSpringSecurity: true

# This might be a custom property of yours if you extended SpringConfiguration.
exampleProperty: This was your example property

########## Everything else is basic Dropwizard ###################

logging:
    level: INFO
    console:
        enabled: true
        threshold: DEBUG
        
http:
    port: 8080
    adminUsername: admin
    adminPassword: admin
    rootPath: /service/*
```

#### Spring Configuration

Wire up all of your JAX-RS Resources, Dropwizard Tasks, Health Checks, Managed Services, etc. in Spring.  Fallwizard will look for classes implementing Dropwizard interfaces or classes and automatically pull them out of the Spring Context and register them with Dropwizard.

### Spring Security Integration

We needed certificated-based client authentication, but didn't want to force BasicAuth when we were already using SSL just so we could inject an object (i.e. `@Auth`) in our JAX-RS controllers.

We also wanted the rich features of Spring Security, which includes support for numerous authentication mechanisms.

__Fallwizard will automatically find the necessary Spring Security components and bind them to Dropwizard if `useSpringSecurity: true` is specified in the yaml configuration__

You simply need to have a Spring Security configuration that looks something like this in your applicationContext.xml:

_Example for Certificate-based authentication_

```xml
<security:http>
  <security:intercept-url pattern="/*" access="ROLE_USER" />
  <security:intercept-url pattern="/admin/*" access="ROLE_ADMIN" />
  <security:x509 subject-principal-regex="CN=(.*?)," />
</security:http>
 
<security:authentication-manager>
  <security:authentication-provider>
    <security:user-service>
      <security:user name="Super Awesome Client" authorities="ROLE_USER" />
      <security:user name="The Boss" authorities="ROLE_USER, ROLE_ADMIN" />
    </security:user-service>
  </security:authentication-provider>
</security:authentication-manager>
```

Once you've got Spring Security working, you can use `@Auth UserDetails userDetails` in your JAX-RS controllers.

Here's an examply of injecting the Spring Security `UserDetails` context into one your controllers (why invent a new `User` object?).

```java
@GET
@Timed
@Path("/chitty-chat/{topic}")
public ChittyChat getChittyChatOnTopic(@Auth UserDetails userDetails, @PathParam("topic") String topic){
   // ... get Chitty-Chat ...
}
```

That's it!

## Using Spring Security without Fallwizard

If you don't like our stuff and prefer to just using Spring Security, you will need to register the `SpringSecurityAuthProvider` with Dropwizard manually.  It looks something like this:

#### Initialize your Spring Application Context.  

Initialize your Spring Application Context in your Dropwizard `Service` class.  We explicitly require the location of the `applicationContext` in our Dropwizard `Configuration` class.

```java
ApplicationContext applicationContext = 
  new FileSystemXmlApplicationContext(
    configuration.getSpringApplicationContext());
```

#### Register Spring Security with the Dropwizard `Environment`.

```java
@Override
public void run(BlahBlahConfiguration configuration, Environment environment) throws Exception {
		
  ApplicationContext applicationContext = 
    new FileSystemXmlApplicationContext(
      configuration.getSpringApplicationContext());
	
  new SpringSecurityAuthProvider(applicationContext).registerProvider(environment);
}
```



## Enjoy a cold beer, you deserve it.

You're done.  Now you can muck with the Spring ApplicationContext "outside" of Dropwizard.
