package com.bericotech.fallwizard;

import com.bericotech.fallwizard.configuration.FallwizardConfiguration;
import com.bericotech.fallwizard.configuration.SpringConfiguration;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public abstract class FallwizardConfiguredCommand extends ConfiguredCommand<FallwizardConfiguration> {

	protected FallwizardConfiguredCommand(String name, String description) {
		
		super(name, description);
	}

	ApplicationContext context;
	
	@Override
	protected void run(
		Bootstrap<FallwizardConfiguration> bootstrap, Namespace namespace,
        FallwizardConfiguration configuration) throws Exception {

        SpringConfiguration springConfig = configuration.getSpringConfiguration();
		// Initialize the Spring Context from the files in the configuration.
		context = new FileSystemXmlApplicationContext(
                springConfig.getApplicationContext());
		
	}
}
