package net.swissartresearch.rds.records;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import com.metaphacts.api.sparql.SparqlOperationBuilder;
import com.metaphacts.config.Configuration;
import com.metaphacts.config.NamespaceRegistry;
import com.metaphacts.plugin.extension.RestExtension;
import com.metaphacts.repository.RepositoryManager;
import com.metaphacts.repository.RepositoryManagerInterface;
import com.metaphacts.secrets.SecretResolver;
import com.metaphacts.secrets.SecretsHelper;

/**
 * REST Endpoint for pushing records from RDS-L to RDS-G
 * 
 * @author wschell
 *
 */
@Singleton
@Path("records")
public class RecordPushEndpoint implements RestExtension {
    private static final Logger logger = LogManager.getLogger(RecordPushEndpoint.class);

    protected NamespaceRegistry namespaceRegistry;
    protected RepositoryManagerInterface repositoryManager;
    protected SecretResolver secretResolver;
    protected RecordPushConfiguration pushConfig;

    public RecordPushEndpoint() {
    }

    @Inject
    public void setNamespaceRegistry(NamespaceRegistry namespaceRegistry) {
        this.namespaceRegistry = namespaceRegistry;
    }

    @Inject
    public void setRepositoryManager(RepositoryManagerInterface repositoryManager) {
        this.repositoryManager = repositoryManager;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        pushConfig = configuration.getCustomConfigurationGroup(RecordPushConfiguration.ID,
                RecordPushConfiguration.class);
    }

    @Inject
    public void setSecretResolver(SecretResolver secretResolver) {
        this.secretResolver = secretResolver;
    }

    /**
     * Push records from RDS-L to RDS-G
     * 
     * @param uriInfo
     * @param subject
     * @param fullName
     * @param emailAddresses
     * @return
     * @throws Exception
     */
    @POST
    @Path("push")
    @RequiresAuthentication
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response pushRecords(@Context UriInfo uriInfo, @FormParam("subject") String subject,
            @FormParam("comment") String comment)
            throws Exception {
        logger.info("Pushing records for subject " + subject + " with comment " + comment);
        logger.debug("UriInfo: {}", uriInfo.getRequestUri());
        
        java.nio.file.Path tempFilePath = Files.createTempFile("merged-graph-", ".ttl");
        File tempFile = tempFilePath.toFile();
        
        logger.debug("Creating local buffer in file {} ", tempFile.getPath());

        RDFFormat format = RDFFormat.TURTLE;
        try (Writer out = new FileWriter(tempFile)) {
            // export data
            exportData(out, format);
        } catch (Exception e) {
            logger.warn("Failed to create local bufer for named graph in file {}: {}", tempFile.getPath(),
                    e.getMessage());
            logger.debug("Details: ", e);
            return Response.serverError().build();
        }

        Optional<String> graphName = Optional.ofNullable(getRemoteGraphName());
        Optional<String> repository = Optional.ofNullable(getRemoteRepository());
        try {
            Entity<?> entity = Entity.entity(tempFile, format.getDefaultMIMEType());

            WebTarget webTarget = getTarget();

            if (repository.isPresent()) {
                webTarget = webTarget.queryParam("repository", repository.get());
            }
            if (graphName.isPresent()) {
                webTarget = webTarget.queryParam("graph", graphName.get());
            }

            // put graph data to remote server
            Response response = webTarget.request().put(entity);
            StatusType statusInfo = response.getStatusInfo();
            
            // delete local buffer file
            if (!tempFile.delete()) {
                logger.warn("Failed to delete local buffer file {}", tempFile.getPath());
            }

            logger.debug("Push result for graph {} in repository {}: {} {}", 
                    graphName.orElse("undefined"), repository.orElse("default"), 
                    statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
            if (statusInfo.getFamily() == Family.SUCCESSFUL) {
                logger.debug("Successfully pushed graph {} in repository {} to upstream server", 
                        graphName.orElse("undefined"), repository.orElse("default"));
                return Response.ok("Pushing done!").build();
            } else {
                logger.warn("Failed to push graph {} in repository {}: {} {}", 
                        graphName.orElse("undefined"), repository.orElse("default"),
                        statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
                return Response.serverError().build();
            }
        } catch (Exception e) {
            logger.warn("Failed to push graph {} in repository {} from file {}: {}", 
                    graphName.orElse("undefined"), repository.orElse("default"),
                    tempFilePath.toString(), e.getMessage());
            logger.debug("Details: ", e);
            return Response.serverError().build();
        }
    }

    protected void exportData(Writer out, RDFFormat format) {
        String queryString = getExportQuery();
        if (queryString == null) {
            logger.warn("No export query provided!");
            throw new IllegalArgumentException("No export query provided!");
        }
        String repositoryID = getLocalRepository();
        if (repositoryID == null) {
            repositoryID = RepositoryManager.DEFAULT_REPOSITORY_ID;
        }
        logger.debug("Exporting data from repository {} using query {}", repositoryID, queryString);
        // query and export data
        Repository repository = repositoryManager.getRepository(repositoryID);
        try (RepositoryConnection con = repository.getConnection()) {
            GraphQuery graphQuery = SparqlOperationBuilder.<GraphQuery>create(queryString, GraphQuery.class)
                                                        .resolveUser(namespaceRegistry.getUserIRI())
                                                        .build(con);
            RDFWriter output = Rio.createWriter(format, out);
            graphQuery.evaluate(output);
        }
    }

    protected WebTarget getTarget() {
        String username = getRemoteServiceUser();
        String password = getRemoteServicePassword();
        String url = getRemoteServiceUrl();

        username = SecretsHelper.resolveSecretOrFallback(secretResolver, username);
        password = SecretsHelper.resolveSecretOrFallback(secretResolver, password);


        WebTarget client = ClientBuilder.newClient().target(url);

        if (username != null && password != null) {
            client.register(HttpAuthenticationFeature.basic(username, password));
        }

        return client;
    }

    protected String getExportQuery() {
        if (pushConfig == null) {
            return null;
        }
        return pushConfig.getExportQuery();
    }

    protected String getRemoteServiceUrl() {
        if (pushConfig == null) {
            return null;
        }
        return pushConfig.getPushTargetUrl();
    }

    protected String getRemoteServiceUser() {
        if (pushConfig == null) {
            return null;
        }
        return pushConfig.getPushTargetUsername();
    }

    protected String getRemoteServicePassword() {
        if (pushConfig == null) {
            return null;
        }
        return pushConfig.getPushTargetPassword();
    }

    protected String getRemoteGraphName() {
        if (pushConfig == null) {
            return null;
        }
        return pushConfig.getPushTargetNamedGraphIRI();
    }

    protected String getRemoteRepository() {
        if (pushConfig == null) {
            return null;
        }
        return pushConfig.getPushTargetRepository();
    }

    protected String getLocalRepository() {
        if (pushConfig == null) {
            return null;
        }
        return pushConfig.getLocalRepository();
    }
}
