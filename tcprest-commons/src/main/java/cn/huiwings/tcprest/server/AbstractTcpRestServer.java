package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.compression.CompressionUtil;
import cn.huiwings.tcprest.parser.DefaultRequestParser;
import cn.huiwings.tcprest.parser.RequestParser;
import cn.huiwings.tcprest.invoker.DefaultInvoker;
import cn.huiwings.tcprest.invoker.Invoker;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.MapperHelper;
import cn.huiwings.tcprest.protocol.ProtocolVersion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Weinan Li
 * @created_at 08 26 2012
 */
public abstract class AbstractTcpRestServer implements TcpRestServer, ResourceRegister {

    protected final Map<String, Mapper> mappers = new HashMap<>(MapperHelper.DEFAULT_MAPPERS);

    protected Logger logger = LoggerFactory.getDefaultLogger();

    protected String status = TcpRestServerStatus.PASSIVE;

    public final Map<String, Class> resourceClasses = new HashMap<String, Class>();

    public final Map<String, Object> singletonResources = new HashMap<String, Object>();

    public RequestParser parser = new DefaultRequestParser(this.getMappers());

    /**
     * V1 invoker field - no longer used by ProtocolRouter.
     * <p>ProtocolRouter now creates its own invoker instances internally
     * to maintain consistency between V1 and V2 component initialization.</p>
     * <p>This field is kept for backward compatibility in case external code references it.</p>
     *
     * @deprecated No longer used internally. ProtocolRouter creates its own invokers.
     */
    @Deprecated
    public Invoker invoker = new DefaultInvoker();

    protected CompressionConfig compressionConfig = new CompressionConfig(); // Default: disabled

    private ProtocolVersion protocolVersion = ProtocolVersion.AUTO; // Default: support both v1 and v2

    /**
     * Protocol router for handling V1 and V2 requests.
     * <p>Initialized when server starts (in up() method) with current configuration.</p>
     * <p>Once initialized, remains constant during server lifetime.</p>
     * <p>AUTO mode: Router handles both V1 and V2 requests by detecting protocol prefix.</p>
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
     * Initialize protocol router with current configuration.
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
            protocolRouter = new ProtocolRouter(
                protocolVersion,
                this,  // Pass 'this' as ResourceRegister for V1 backward compatibility
                mappers,
                compressionConfig,
                logger
            );
            logger.info("Protocol router initialized with version: " + protocolVersion);
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

        // Route request to appropriate protocol handler (V1 or V2)
        // In AUTO mode, router detects version from request prefix and routes accordingly
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
     * Enable compression with default settings
     */
    public void enableCompression() {
        this.compressionConfig.setEnabled(true);
        logger.info("Compression enabled with default settings");
    }

    /**
     * Disable compression
     */
    public void disableCompression() {
        this.compressionConfig.setEnabled(false);
        logger.info("Compression disabled");
    }

    /**
     * Set protocol version for the server.
     *
     * <p><b>IMPORTANT:</b> This should be called BEFORE starting the server (before {@code up()}).
     * Changing protocol version after server is running will reset the router on next request,
     * causing a small performance penalty.</p>
     *
     * <p><b>Protocol Version Options:</b></p>
     * <ul>
     *   <li><b>AUTO</b> (default): Server accepts both V1 and V2 clients. Recommended for:
     *       <ul>
     *         <li>Migration period (supporting old V1 and new V2 clients)</li>
     *         <li>Public APIs with mixed client versions</li>
     *         <li>Backward compatibility requirements</li>
     *       </ul>
     *   </li>
     *   <li><b>V2</b>: Only accept V2 clients. Use when:
     *       <ul>
     *         <li>All clients upgraded to V2</li>
     *         <li>New projects (no V1 legacy)</li>
     *       </ul>
     *   </li>
     *   <li><b>V1</b>: Only accept V1 clients. Use for:
     *       <ul>
     *         <li>Legacy systems</li>
     *         <li>Backward compatibility</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @param version protocol version (V1, V2, or AUTO)
     */
    public void setProtocolVersion(ProtocolVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("Protocol version cannot be null");
        }
        if (protocolRouter != null) {
            logger.warn("Changing protocol version after router initialization. " +
                       "Router will be reset on next request. " +
                       "Recommend setting protocol version before calling up().");
        }
        this.protocolVersion = version;
        this.protocolRouter = null; // Reset router to pick up new version
        logger.info("Protocol version set to: " + version);
    }

    /**
     * Get current protocol version.
     *
     * @return protocol version
     */
    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
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
