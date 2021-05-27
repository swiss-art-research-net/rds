package net.swissartresearch.rds.records;

import javax.inject.Inject;

import com.metaphacts.config.ConfigurationParameter;
import com.metaphacts.config.InvalidConfigurationException;
import com.metaphacts.plugin.extension.ConfigurationExtension;
import com.metaphacts.services.storage.api.PlatformStorage;

/**
 * Config class for options required to create and push a graph to a remote system
 * 
 * <p>
 * Use the following snippet to show the configuration on the UI:
 * </p>
 * <pre>
 * <mp-collapsible-div expanded=false>
 *   <mp-collapsible-div-trigger>
 *     <span>Record Push Configuration</span>
 *   </mp-collapsible-div-trigger>
 *   <mp-collapsible-div-content>
 *     <mp-admin-config-manager editable=true group="rds-services"></mp-admin-config-manager>
 *   </mp-collapsible-div-content>
 * </mp-collapsible-div>
 * </pre>
 * @author wschell
 */
public class RdsServicesConfiguration extends ConfigurationExtension {
    public static final String ID = "rds-services";
    public static final String DESCRIPTION = "Push configuration for RDS";

    @Inject
    public RdsServicesConfiguration(PlatformStorage platformStorage) throws InvalidConfigurationException {
        super(ID, DESCRIPTION, platformStorage);
    }

    @ConfigurationParameter(name = "pushTargetUrl", restartRequired = false, desc = "URL of the upstream target for pushing records.")
    public String getPushTargetUrl() {
        return getString("pushTargetUrl");
    }

    @ConfigurationParameter(name = "pushTargetUsername", restartRequired = false, desc = "Username of the upstream target for pushing records.")
    public String getPushTargetUsername() {
        return getString("pushTargetUsername");
    }

    @ConfigurationParameter(name = "pushTargetPassword", restartRequired = false, desc = "Password of the upstream target for pushing records.")
    public String getPushTargetPassword() {
        return getString("pushTargetPassword");
    }

    @ConfigurationParameter(name = "pushTargetRepository", restartRequired = false, desc = "Name of the upstream repository for pushing records. If unset the default repository is used.")
    public String getPushTargetRepository() {
        return getString("pushTargetRepository");
    }

    @ConfigurationParameter(name = "pushTargetGraphName", restartRequired = false, desc = "Fully-qualified IRI of the named graph to push records to (without enclosing <>).")
    public String getPushTargetGraphName() {
        return getString("pushTargetGraphName");
    }

    @ConfigurationParameter(name = "localRepository", restartRequired = false, desc = "Name of the local repository from which to export data. If unset the default repository is used.")
    public String getLocalRepository() {
        return getString("localRepository");
    }

    @ConfigurationParameter(name = "exportQuery", restartRequired = false, desc = "SPARQL CONSTRUCT query which returns the RDF statements to be exported")
    public String getExportQuery() {
        return getString("exportQuery");
    }

    @ConfigurationParameter(name = "pushQuery", restartRequired = false, desc = "SPARQL CONSTRUCT query which returns the RDF statements to be pushed")
    public String getPushQuery() {
        return getString("pushQuery");
    }

    @Override
    public void assertConsistency() {
    }
}
