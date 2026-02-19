package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.codec.ProtocolCodec;
import cn.huiwings.tcprest.codec.v2.ProtocolV2Codec;
import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.exception.BusinessException;
import cn.huiwings.tcprest.exception.ProtocolException;
import cn.huiwings.tcprest.invoker.v2.ProtocolV2Invoker;
import java.util.logging.Logger;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.MapperHelper;
import cn.huiwings.tcprest.parser.RequestParser;
import cn.huiwings.tcprest.parser.v2.ProtocolV2Parser;
import cn.huiwings.tcprest.protocol.v2.ProtocolV2Constants;
import cn.huiwings.tcprest.protocol.v2.StatusCode;
import cn.huiwings.tcprest.security.SecurityConfig;

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

    protected Logger logger = Logger.getLogger(AbstractTcpRestServer.class.getName());

    protected String status = TcpRestServerStatus.PASSIVE;

    public final Map<String, Class> resourceClasses = new HashMap<String, Class>();

    public final Map<String, Object> singletonResources = new HashMap<String, Object>();

    protected CompressionConfig compressionConfig = new CompressionConfig(); // Default: disabled

    /**
     * Protocol V2 components - initialized when server starts.
     * <p>Initialized in {@link #initializeProtocolComponents()} when server starts (in up() method).</p>
     * <p>Once initialized, remain constant during server lifetime.</p>
     */
    private volatile RequestParser parser;
    private volatile ProtocolV2Invoker invoker;
    private volatile ProtocolCodec codec;

    public void addResource(Class resourceClass) {
        if (resourceClass == null) {
            return;
        }

        // Adding multiple instances of same class is meaningless. So every TcpRestServer implementation
        // should check and overwrite existing instances of same class and give out warning each time a
        // singleton resource is added.
        synchronized (resourceClasses) {
            if (resourceClasses.containsKey(resourceClass.getCanonicalName())) {
                logger.warning("Resource already exists for: " + resourceClass.getCanonicalName());
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

    /**
     * Initialize Protocol V2 components with current configuration.
     * <p>Should be called by subclasses in their {@code up()} method to initialize
     * protocol components before accepting requests.</p>
     *
     * <p>Example usage in subclass:</p>
     * <pre>
     * public void up() {
     *     initializeProtocolComponents();  // Initialize components first
     *     // Then start accepting connections...
     * }
     * </pre>
     */
    protected void initializeProtocolComponents() {
        if (parser == null) {
            parser = new ProtocolV2Parser(mappers);
            invoker = new ProtocolV2Invoker();
            codec = new ProtocolV2Codec(mappers);

            // Apply security config if it was set before initialization
            if (securityConfig != null) {
                ((ProtocolV2Parser) parser).setSecurityConfig(securityConfig);
                ((ProtocolV2Codec) codec).setSecurityConfig(securityConfig);
            }

            logger.info("Protocol V2 components initialized");
        }
    }

    /**
     * Process Protocol V2 request with comprehensive exception handling.
     *
     * <p><b>Request Format:</b></p>
     * <pre>
     * V2|0|{{base64(ClassName/methodName(TYPE_SIGNATURE))}}|[param1,param2]|CHK:value
     * </pre>
     *
     * <p><b>Response Format:</b></p>
     * <pre>
     * V2|0|STATUS|{{base64(BODY)}}|CHK:value
     * </pre>
     *
     * @param request the V2 request string
     * @return V2 response string with status code
     * @throws Exception if request processing fails critically
     */
    protected String processRequest(String request) throws Exception {
        logger.fine("request: " + request);

        // Components should be initialized in up() method via initializeProtocolComponents()
        // If not initialized (edge case), initialize now
        if (parser == null) {
            logger.warning("Protocol components not initialized - initializing now. " +
                       "Consider calling initializeProtocolComponents() in up() method.");
            initializeProtocolComponents();
        }

        // Validate request
        if (request == null || request.isEmpty()) {
            return handleError(new ProtocolException("Empty request"));
        }

        // Validate protocol version
        if (!request.startsWith(ProtocolV2Constants.PREFIX)) {
            return handleError(new ProtocolException(
                "Only Protocol V2 is supported. Request must start with '" +
                ProtocolV2Constants.PREFIX + "'"
            ));
        }

        // Process V2 request
        try {
            // Parse request into context
            Context context = parser.parse(request);

            // Resolve resource instance
            Class<?> targetClass = context.getTargetClass();
            Object instance = ResourceResolver.findResourceInstance(targetClass, this, logger);
            context.setTargetInstance(instance);

            // Invoke method
            Object result = invoker.invoke(context);

            // Encode success response
            return ((ProtocolV2Codec) codec).encodeResponse(result, StatusCode.SUCCESS);

        } catch (BusinessException e) {
            // Business exception - expected error from business logic
            logger.warning("Business exception: " + e.getMessage());
            return ((ProtocolV2Codec) codec).encodeException(e, StatusCode.BUSINESS_EXCEPTION);

        } catch (cn.huiwings.tcprest.exception.SecurityException e) {
            // Security violation - checksum failure, whitelist block, etc.
            logger.severe("Security violation: " + e.getMessage());
            return ((ProtocolV2Codec) codec).encodeException(e, StatusCode.PROTOCOL_ERROR);

        } catch (ProtocolException e) {
            // Protocol error - malformed request or parsing failure
            logger.severe("Protocol error: " + e.getMessage());
            return ((ProtocolV2Codec) codec).encodeException(e, StatusCode.PROTOCOL_ERROR);

        } catch (Exception e) {
            // Server error - unexpected exception during processing
            logger.severe("Server error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return ((ProtocolV2Codec) codec).encodeException(e, StatusCode.SERVER_ERROR);
        }
    }

    /**
     * Handle errors and return appropriate error response.
     *
     * @param error the error
     * @return error response
     */
    private String handleError(Exception error) {
        return ((ProtocolV2Codec) codec).encodeException(error, StatusCode.PROTOCOL_ERROR);
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

    private SecurityConfig securityConfig;

    /**
     * Set security configuration for protocol components.
     *
     * @param securityConfig security configuration
     */
    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
        if (securityConfig != null) {
            if (parser instanceof ProtocolV2Parser) {
                ((ProtocolV2Parser) parser).setSecurityConfig(securityConfig);
            }
            if (codec instanceof ProtocolV2Codec) {
                ((ProtocolV2Codec) codec).setSecurityConfig(securityConfig);
            }
        }
    }

    /**
     * Get security configuration.
     *
     * @return security configuration
     */
    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    /**
     * Get the request parser.
     *
     * @return request parser
     */
    public RequestParser getParser() {
        return parser;
    }

    /**
     * Get the protocol codec.
     *
     * @return protocol codec
     */
    public ProtocolCodec getCodec() {
        return codec;
    }

    /**
     * Get the method invoker.
     *
     * @return method invoker
     */
    public ProtocolV2Invoker getInvoker() {
        return invoker;
    }

}
