package com.bericotech.fallwizard.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * This is the overall configuration for the FallWizard project.
 *
 * History:  12/14/2013 Separates the spring configuration from the overall configuration.
 *           This is in better keeping with DropWizard conventions and allows cleaner separation.
 *           Now the application context files, and various other spring features (e.g. properties)
 *           are better delineated.
 *
 * @author Richard Clayton
 * @author Justin McCune (v1.2.0)
 * Date: 12/14/13
 */
public class FallwizardConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("spring")
    private SpringConfiguration springConfiguration = new SpringConfiguration();

    public SpringConfiguration getSpringConfiguration() {
        return springConfiguration;
    }
}
