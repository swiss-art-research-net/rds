package net.swissartresearch.rds.records;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.metaphacts.repository.MpRepositoryProvider;
import com.metaphacts.security.PlatformTaskWrapper;
import com.metaphacts.services.files.FileManager;
import com.metaphacts.services.files.ManagedFileName;
import com.metaphacts.services.storage.api.ObjectKind;
import com.metaphacts.services.storage.api.ObjectMetadata;
import com.metaphacts.services.storage.api.ObjectRecord;
import com.metaphacts.services.storage.api.ObjectStorage;
import com.metaphacts.services.storage.api.PlatformStorage;
import com.metaphacts.services.storage.api.SizedStream;
import com.metaphacts.services.storage.api.StoragePath;
import com.metaphacts.services.storage.utils.ExactSizeInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.Update;
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
 * REST Endpoint for pushing/exporting records from RDS-L to RDS-G
 * 
 * @author wschell
 *
 */
@Singleton
@Path("records")
public class RdsServicesEndpoint implements RestExtension, AutoCloseable {
    private static final Logger logger = LogManager.getLogger(RdsServicesEndpoint.class);
    private static final int THREAD_POOL_SIZE = 20;
    private static final String FILE_STORAGE_ID = "exportedRecords";
    private static final String FILE_NAME = "exportedRecords.ttl";
    private static final String REPOSITORY_ID = "default";
    private static final String MEDIA_TYPE = "ttl";
    private static final String CONTEXT_URI = "http://rds-extension.com/exportedRecords/context";
    private static final String GENERATE_IRI_QUERY = "SELECT ?resourceIri WHERE {\n" +
        "BIND(URI(CONCAT(STR(?__contextUri__), \"/\", ?__fileName__)) as ?resourceIri)\n" +
    "}";
    private static final String CREATE_RESOURCE_QUERY = "CONSTRUCT {\n" +
    "  ?__resourceIri__ a <http://www.metaphacts.com/ontologies/platform#File>.\n" +
    "  ?__resourceIri__ <http://www.w3.org/2000/01/rdf-schema#label> ?__fileName__.\n" +
    "  ?__resourceIri__ <http://www.metaphacts.com/ontologies/platform#mediaType> ?__mediaType__.\n" +
    "  ?__resourceIri__ <http://www.metaphacts.com/ontologies/platform#context> ?__contextUri__.\n" +
    "} WHERE {}";
    private static final String CREATE_RESOURCE_QUERY_READY = "CONSTRUCT {\n" +
    "  ?__resourceIri__ a <http://www.metaphacts.com/ontologies/platform#File>.\n" +
    "  ?__resourceIri__ <http://www.w3.org/2000/01/rdf-schema#label> ?__fileName__.\n" +
    "  ?__resourceIri__ <http://www.metaphacts.com/ontologies/platform#mediaType> ?__mediaType__.\n" +
    "  ?__resourceIri__ <http://www.metaphacts.com/ontologies/platform#context> ?__contextUri__.\n" +
    "  ?__resourceIri__ <http://www.metaphacts.com/ontologies/platform#readyToDownload> true.\n" +
    "} WHERE {}";

    protected ExecutorService executorService;
    
    protected NamespaceRegistry namespaceRegistry;
    protected RepositoryManager repositoryManager;
    protected SecretResolver secretResolver;
    protected RdsServicesConfiguration servicesConfig;
    protected PlatformStorage platformStorage;
    protected FileManager fileManager = new FileManager();

    public RdsServicesEndpoint() {}

    @Inject
    public void setNamespaceRegistry(NamespaceRegistry namespaceRegistry) {
        this.namespaceRegistry = namespaceRegistry;
    }

    @Inject
    public void setRepositoryManager(RepositoryManagerInterface repositoryManager) {
        this.repositoryManager = (RepositoryManager) repositoryManager;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        servicesConfig = configuration.getCustomConfigurationGroup(RdsServicesConfiguration.ID,
                RdsServicesConfiguration.class);
    }

    @Inject
    public void setSecretResolver(SecretResolver secretResolver) {
        this.secretResolver = secretResolver;
    }

    @Inject
    public void setPlatformStorage(PlatformStorage platformStorage) {
        this.platformStorage = platformStorage;
    }

    /**
     * Push records from RDS-L to RDS-G
     * 
     * @param uriInfo
     * @param subject
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

    /**
     * Push records from RDS-L to RDS-G
     *
     * @param uriInfo
     * @param subject
     * @return
     * @throws Exception
     */
    @POST
    @Path("export")
    @RequiresAuthentication
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response exportRecords(
        @Context UriInfo uriInfo,
        @FormParam("subject") String subject,
        @FormParam("comment") String comment
    ) throws Exception {
        logger.info("Exporting records for subject " + subject + " with comment " + comment);
        logger.debug("UriInfo: {}", uriInfo.getRequestUri());
        
        ManagedFileName managedName = ManagedFileName.generateFromFileName(
                ObjectKind.FILE, FILE_NAME, fileManager::generateSequenceNumber);
        this.updateMetadata(managedName, false);
        this.startDownloading(managedName);
        
        return Response.ok().build();
    }

    public synchronized CompletableFuture<StreamingOutput> startDownloading(ManagedFileName managedName) {
        CompletableFuture<StreamingOutput> completableFuture = new CompletableFuture<>();
        getExecutorService().submit(() -> {
            try {
                completableFuture.complete(this.downloadFileTask(managedName));
            } catch (Throwable t) {
                logger.warn("Error while downloading file: " + t.getMessage());
                logger.debug("details:", t);
                completableFuture.completeExceptionally(t);
            }
        });
        return completableFuture;
    }

    @Override
    public synchronized void close() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }
    
    protected synchronized ExecutorService getExecutorService() {
        if (this.executorService == null) {
            this.executorService = Executors.newFixedThreadPool(
                THREAD_POOL_SIZE,
                new ThreadFactoryBuilder().setNameFormat("records-export-%d").build()
            );
        }
        return this.executorService;
    }

    protected StreamingOutput downloadFileTask(ManagedFileName managedName) throws IOException {
        this.storeFile(managedName);
        this.updateMetadata(managedName, true);
        ObjectStorage storage = this.platformStorage.getStorage(FILE_STORAGE_ID);
        ObjectRecord records = this.fileManager.fetchFile(storage, managedName).get();
        return readObjectContent(records);
    }
    
    protected void storeFile(ManagedFileName managedName) throws IOException {
        java.nio.file.Path tempFilePath = Files.createTempFile("merged-graph-", ".ttl");
        File file = tempFilePath.toFile();
        RDFFormat format = RDFFormat.TURTLE;
        try (Writer out = new FileWriter(file)) {
            // export data
            exportData(out, format);
            ObjectStorage storage = this.platformStorage.getStorage(FILE_STORAGE_ID);

            FileInputStream inputStream = new FileInputStream(file);
            SizedStream stream = new SizedStream(inputStream, inputStream.getChannel().size());
            this.fileManager.storeFile(
                storage,
                managedName,
                platformStorage.getDefaultMetadata(),
                stream
            );
        } catch (Exception e) {
            logger.warn("Failed to create file locally for named graph in file {}: {}", file.getPath(), e.getMessage());
            logger.debug("Details: ", e);
        }
    }

    protected IRI updateMetadata(ManagedFileName managedName, boolean ready) throws IOException {
        ObjectStorage storage = this.platformStorage.getStorage(FILE_STORAGE_ID);
        
        IRI resourceIri;
        try {
            resourceIri = fileManager.createLdpResource(
                    managedName,
                    new MpRepositoryProvider(this.repositoryManager, REPOSITORY_ID),
                    GENERATE_IRI_QUERY,
                    ready ? CREATE_RESOURCE_QUERY_READY : CREATE_RESOURCE_QUERY,
                    CONTEXT_URI,
                    MEDIA_TYPE
            );
        } catch (Exception e) {
            // try to clean up uploaded file if LDP update failed
            fileManager.deleteFile(storage, managedName, platformStorage.getDefaultMetadata());
            throw e;
        }

        return resourceIri;
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
        if (servicesConfig == null) {
            return null;
        }
        return servicesConfig.getExportQuery();
    }

    protected String getRemoteServiceUrl() {
        if (servicesConfig == null) {
            return null;
        }
        return servicesConfig.getPushTargetUrl();
    }

    protected String getRemoteServiceUser() {
        if (servicesConfig == null) {
            return null;
        }
        return servicesConfig.getPushTargetUsername();
    }

    protected String getRemoteServicePassword() {
        if (servicesConfig == null) {
            return null;
        }
        return servicesConfig.getPushTargetPassword();
    }

    protected String getRemoteGraphName() {
        if (servicesConfig == null) {
            return null;
        }
        return servicesConfig.getPushTargetNamedGraphIRI();
    }

    protected String getRemoteRepository() {
        if (servicesConfig == null) {
            return null;
        }
        return servicesConfig.getPushTargetRepository();
    }

    protected String getLocalRepository() {
        if (servicesConfig == null) {
            return null;
        }
        return servicesConfig.getLocalRepository();
    }

    protected StreamingOutput readObjectContent(ObjectRecord record) {
        return output -> {
            try (InputStream content = record.getLocation().readContent()) {
                content.transferTo(output);
            }
            output.flush();
        };
    }
}
