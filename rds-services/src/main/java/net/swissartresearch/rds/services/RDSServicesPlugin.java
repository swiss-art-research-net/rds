package net.swissartresearch.rds.services;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import com.google.inject.Injector;
import com.metaphacts.config.Configuration;
import com.metaphacts.config.InvalidConfigurationException;
import com.metaphacts.services.storage.api.PlatformStorage;

import net.swissartresearch.rds.records.RdsServicesConfiguration;

public class RDSServicesPlugin extends Plugin {
    private static final Logger logger = LogManager.getLogger(RDSServicesPlugin.class);

    protected Configuration configuration;
    protected PlatformStorage platformStorage;
    protected Injector injector;

    public RDSServicesPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Inject
    public void setPlatformStorage(PlatformStorage platformStorage) {
        this.platformStorage = platformStorage;
    }

    @Inject
    public void setInjector(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void start() {
        logger.debug("Starting plugin");

        if ((configuration != null) && (platformStorage != null)) {
            try {
                logger.info("Registering record services config");
                // NOTE: ideally we would have had this properly detected and injected, but
                // unfortunately the PluginFinder for PF4J extensions only loads these instances
                // when the plugin class is started which is too late in the startup so the
                // config class will not be loaded properly.
                // As a workaround we explicitly create and register the configuration here.
                RdsServicesConfiguration pushConfig = new RdsServicesConfiguration(platformStorage);
                // ensure all dependencies are provided
                injector.injectMembers(pushConfig);

                configuration.registerCustomConfigurationGroup(pushConfig);
            } catch (InvalidConfigurationException e) {
                logger.warn("Failed to load record push config: " + e.getMessage());
                logger.debug("Details: ", e);
                throw new RuntimeException("Failed to load record push config: " + e.getMessage(), e);
            }
        }

        super.start();
    }

    @Override
    public void stop() {
        logger.debug("Stopping plugin");
        super.stop();
    }
}
