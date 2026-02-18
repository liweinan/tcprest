package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.MapperHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for TcpRest server implementations.
 *
 * <p>Provides common functionality for resource management, mapper configuration,
 * and Protocol V2 request processing.</p>
 *
 * <p><b>Protocol Support:</b> Protocol V2 only (V1 removed in version 2.0.0)</p>
 *
 * @author Weinan Li
 * @created_at 08 26 2012
 */
public abstract class AbstractTcpRestServer implements TcpRestServer, ResourceRegister {

    protected final Map<String, Mapper> mappers = new HashMap<>(MapperHelper.DEFAULT_MAPPERS);

    protected Logger logger = LoggerFactory.getDefaultLogger();

    protected String status = TcpRestServerStatus.PASSIVE;

    public final Map<String, Class> resourceClasses = new HashMap<String, Class>();

    public final Map<String, Object> singletonResources = new HashMap<String, Object>();

    protected CompressionConfig compressionConfig = new CompressionConfig(); // Default: disabled

    /**
     * Protocol router for handling V2 requests.
     * <p>Initialized when server starts (in up() method) with current configuration.</p>
     * <p>Once initialized, remains constant during server lifetime.</p>
     */
    private volatile ProtocolRouter protocolRouter;

    public void addResource(Class resourceClass) {
        if (resourceClass == null) {
            return;
        }

        // Adding multiple instances of same class is meaningless. So every TcpRestServer implementation
        // should check and overwrite existing instances of same class and give out warning each time a
        // singleton resource is added.
        synchronized (resourceClasses) {
            if (resourceClasses.containsKey(resourceClass.getCanonicalName())) {
                logger.warn("Resource already exists for: " + resourceClass.getCanonicalName());
            }
            deleteResource(resourceClass);
            resourceClasses.put(resourceClass.getCanonicalName(), resourceClass);
        }
    }

    public void deleteResource(Class resourceClass) {
        synchronized (resourceClasses) {
            resourceClasses.remove(resourceClass.getCanonicalName());
        }
    }

    public void addSingletonResource(Object instance) {
        synchronized (singletonResources) {
            deleteSingletonResource(instance);
            singletonResources.put(instance.getClass().getCanonicalName(), instance);
        }
    }

    public void deleteSingletonResource(Object instance) {
        synchronized (singletonResources) {
            singletonResources.remove(instance.getClass().getCanonicalName());
        }
    }

    public Map<String, Class> getResourceClasses() {
        return new HashMap<String, Class>(resourceClasses);
    }

    public Map<String, Object> getSingletonResources() {
        return new HashMap<String, Object>(singletonResources);
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Initialize Protocol V2 router with current configuration.
     * <p>Should be called by subclasses in their {@code up()} method to initialize
     * the router before accepting requests.</p>
     *
     * <p>Example usage in subclass:</p>
     * <pre>
     * public void up() {
     *     initializeProtocolRouter();  // Initialize router first
     *     // Then start accepting connections...
     * }
     * </pre>
     */
    protected void initializeProtocolRouter() {
        if (protocolRouter == null) {
            protocolRouter = new ProtocolRouter(mappers, logger);
            logger.info("Protocol V2 router initialized");
        }
    }

    protected String processRequest(String request) throws Exception {
        logger.debug("request: " + request);

        // Router should be initialized in up() method via initializeProtocolRouter()
        // If not initialized (edge case), initialize now
        if (protocolRouter == null) {
            logger.warn("Protocol router not initialized - initializing now. " +
                       "Consider calling initializeProtocolRouter() in up() method.");
            initializeProtocolRouter();
        }

        // Process V2 request
        return protocolRouter.processRequest(request, this);
    }

    public Map<String, Mapper> getMappers() {
        // We don't want user to modify the mappers by getMappers.
        // Use addMapper() to add mapper to server to ensure concurrency safety
        return new HashMap<String, Mapper>(this.mappers);
    }

    public void addMapper(String canonicalName, Mapper mapper) {
        synchronized (mappers) {
            mappers.put(canonicalName, mapper);
        }
    }

    public CompressionConfig getCompressionConfig() {
        return compressionConfig;
    }

    public void setCompressionConfig(CompressionConfig compressionConfig) {
        if (compressionConfig == null) {
            throw new IllegalArgumentException("Compression config cannot be null");
        }
        this.compressionConfig = compressionConfig;
        logger.info("Compression configured: " + compressionConfig);
    }

    /**
     * Enable compression with default settings.
     */
    public void enableCompression() {
        this.compressionConfig.setEnabled(true);
        logger.info("Compression enabled with default settings");
    }

    /**
     * Disable compression.
     */
    public void disableCompression() {
        this.compressionConfig.setEnabled(false);
        logger.info("Compression disabled");
    }

    /**
     * Get resource instance by class name (ResourceRegister interface).
     *
     * @param className fully qualified class name
     * @return resource instance, or null if not found
     */
    @Override
    public Object getResource(String className) {
        // Check singleton resources first
        Object singleton = singletonResources.get(className);
        if (singleton != null) {
            return singleton;
        }

        // Resource classes require instantiation (handled by invoker)
        return null;
    }

    /**
     * Check if resource is registered (ResourceRegister interface).
     *
     * @param className fully qualified class name
     * @return true if registered
     */
    @Override
    public boolean hasResource(String className) {
        return singletonResources.containsKey(className) ||
               resourceClasses.containsKey(className);
    }

}
