package com.bericotech.fallwizard.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * User: jmccune
 * Date: 12/15/13
 */
public class SpringPropertyPlaceholderConfigurerConfiguration extends Configuration {

    private static final Logger logger=LoggerFactory.getLogger(SpringPropertyPlaceholderConfigurerConfiguration.class);
    // ---- Behavior Variables (defaults usually reasonable) ---

    /**
     * Should a property directly in the YAML configuration for this object
     * override one found in a property file (specified in the YAML locations for this object)?
     * Defaults to normal behavior of the configurer if not specified.
     */
    @Valid
    @JsonProperty
    private Boolean localOverride=null;


    /** For interpreting traditional properties files, what encoding should be used.
     * If not specified, the default properties (object) encoding is used.
     */
    @Valid
    @JsonProperty
    private String fileEncoding=null;


    /**
     * Are properties file resource specifications optional?
     * (E.g. if not found is it an error?)
     * * Defaults to nor+mal behavior of the configurer if not specified.
     */
    @Valid
    @JsonProperty
    private Boolean ignoreResourceNotFound=null;

    /** Allow the yaml file to redefine which class provides the implementation.   */
    @Valid
    @JsonProperty
    private String propertyPlaceholderClassName=
            "org.springframework.beans.factory.config.PropertyPlaceholderConfigurer";


    @Valid
    @JsonProperty
    private Boolean ignoreUnresolvablePlaceholders=null;


    @Valid
    @JsonProperty
    private Integer order=null;

    // ---- Property Resources, one of these must be defined ---

    @Valid
    @JsonProperty
    private Map<String,String> properties =null;

    @Valid
    @JsonProperty
    private List<String> locations =null;


    // --------------------------------------------------------------
    // Create the desired property configurer.
    // --------------------------------------------------------------
    public PropertyPlaceholderConfigurer createPlaceholderConfigurer(GenericApplicationContext applicationContext) {

        // -- Create the object --
        PropertyPlaceholderConfigurer implementation;
        try {
            Class<?> classType = this.getClass().getClassLoader().loadClass(propertyPlaceholderClassName);
            implementation = (PropertyPlaceholderConfigurer) classType.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal configuration argument: " + propertyPlaceholderClassName, e);
        }


        // -- Configure the behavior of the object --
        if (fileEncoding!=null) {
            implementation.setFileEncoding(fileEncoding);
        }

        if (ignoreResourceNotFound!=null) {
            implementation.setIgnoreResourceNotFound(ignoreResourceNotFound);
        }

        if (localOverride!=null) {
            implementation.setLocalOverride(localOverride);
        }

        if (ignoreUnresolvablePlaceholders!=null) {
            implementation.setIgnoreUnresolvablePlaceholders(ignoreUnresolvablePlaceholders);
        }

        if (order!=null) {
            implementation.setOrder(order);
        }
        // -- Load the properties --

        // - Locations -
        if (locations!=null && !locations.isEmpty()) {
            List<Resource> resourceLocations = new ArrayList<>();
            for (String locationSpec : locations) {
                logger.info(">>  Location Specification: "+locationSpec);
                Resource resource = applicationContext.getResource(locationSpec);
                logger.info("Converted to resource: "+resource);
                resourceLocations.add(resource);
            }
            Resource[] resourceArray = new Resource[resourceLocations.size()];
            resourceArray = resourceLocations.toArray(resourceArray);
            implementation.setLocations(resourceArray);
        }

        // - Properties -
        if (properties !=null && !properties.isEmpty()) {
            Properties props = new Properties();
            props.putAll(properties);
            implementation.setProperties(props);
        }


        return implementation;
    }


    // --------------------------------------------------------------
    // Plain Old Getters
    // --------------------------------------------------------------
    public Map<String, String> getProperties() {
        return properties;
    }

    public List<String> getLocations() {
        return locations;
    }


    public String getPropertyPlaceholderClassName() {
        return propertyPlaceholderClassName;
    }

    public Boolean isLocalOverride() {
        return localOverride;
    }

    public Boolean isIgnoreResourceNotFound() {
        return ignoreResourceNotFound;
    }

    public String getFileEncoding() {
        return fileEncoding;
    }

    public Boolean isIgnoreUnresolvablePlaceholders() {
        return ignoreUnresolvablePlaceholders;
    }
}



