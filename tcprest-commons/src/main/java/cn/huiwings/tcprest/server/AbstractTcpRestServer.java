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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
public abstract class AbstractTcpRestServer implements TcpRestServer {

    protected final Map<String, Mapper> mappers = new HashMap<>(MapperHelper.DEFAULT_MAPPERS);

    protected Logger logger = Logger.getLogger(AbstractTcpRestServer.class.getName());

    protected volatile String status = TcpRestServerStatus.CLOSED;

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

    /**
     * When true, addResource/addSingletonResource throw if any DTO type is neither
     * Serializable nor has a mapper. When false (default), only a warning is logged.
     */
    private boolean strictTypeCheck = false;

    @Override
    public void setStrictTypeCheck(boolean strictTypeCheck) {
        this.strictTypeCheck = strictTypeCheck;
    }

    @Override
    public boolean isStrictTypeCheck() {
        return strictTypeCheck;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
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
        validateResourceTypes(resourceClass);
    }

    @Override
    public void deleteResource(Class resourceClass) {
        synchronized (resourceClasses) {
            resourceClasses.remove(resourceClass.getCanonicalName());
        }
    }

    @Override
    public void addSingletonResource(Object instance) {
        if (instance == null) {
            return;
        }
        synchronized (singletonResources) {
            deleteSingletonResource(instance);
            singletonResources.put(instance.getClass().getCanonicalName(), instance);
        }
        validateResourceTypes(instance.getClass());
    }

    @Override
    public void deleteSingletonResource(Object instance) {
        synchronized (singletonResources) {
            singletonResources.remove(instance.getClass().getCanonicalName());
        }
    }

    @Override
    public Map<String, Class> getResourceClasses() {
        return new HashMap<String, Class>(resourceClasses);
    }

    @Override
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
        logger.fine("request: " + sanitizeForLog(request));

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
        return encodeErrorResponse(error, StatusCode.PROTOCOL_ERROR);
    }

    /**
     * Encode an exception as a V2 error response. For use by transport layers (e.g. UDP handler).
     *
     * @param error the exception
     * @param status the status code (e.g. SERVER_ERROR for processing failures)
     * @return V2 response string
     */
    protected String encodeErrorResponse(Throwable error, StatusCode status) {
        return ((ProtocolV2Codec) codec).encodeException(error, status);
    }

    @Override
    public Map<String, Mapper> getMappers() {
        // We don't want user to modify the mappers by getMappers.
        // Use addMapper() to add mapper to server to ensure concurrency safety
        return new HashMap<String, Mapper>(this.mappers);
    }

    @Override
    public void addMapper(String canonicalName, Mapper mapper) {
        synchronized (mappers) {
            mappers.put(canonicalName, mapper);
        }
    }

    @Override
    public CompressionConfig getCompressionConfig() {
        return compressionConfig;
    }

    @Override
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
    @Override
    public void enableCompression() {
        this.compressionConfig.setEnabled(true);
        logger.info("Compression enabled with default settings");
    }

    /**
     * Disable compression.
     */
    @Override
    public void disableCompression() {
        this.compressionConfig.setEnabled(false);
        logger.info("Compression disabled");
    }

    /**
     * Get resource instance by class name.
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
     * Check if resource is registered.
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
    @Override
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
    @Override
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

    /**
     * Validate that all DTO types used by the resource are either Serializable or have a mapper.
     * Logs warning for each unsupported type; if {@link #strictTypeCheck} is true, throws instead.
     *
     * @param resourceClass the resource class to validate
     * @throws IllegalStateException if strictTypeCheck is true and any unsupported type is found
     */
    protected void validateResourceTypes(Class<?> resourceClass) {
        List<String> unsupported = collectUnsupportedTypes(resourceClass, mappers);
        if (unsupported.isEmpty()) {
            return;
        }
        String message = "Resource " + resourceClass.getCanonicalName()
            + " uses types that are neither Serializable nor have a mapper; they will be serialized as toString() and may cause ClassCastException/IllegalArgumentException: "
            + unsupported;
        if (strictTypeCheck) {
            throw new IllegalStateException(message);
        }
        logger.warning(message);
    }

    /**
     * Collect fully qualified type names that are not supported (no Serializable, no mapper).
     * Matches Protocol V2 semantics: primitives, String, wrappers, collection interfaces,
     * primitive/String arrays are supported; other types need Serializable or mapper.
     */
    private static List<String> collectUnsupportedTypes(Class<?> resourceClass, Map<String, Mapper> mappers) {
        List<String> unsupported = new ArrayList<>();
        for (java.lang.reflect.Method method : resourceClass.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            for (Class<?> paramType : method.getParameterTypes()) {
                if (!isTypeSupported(paramType, mappers)) {
                    String name = paramType.getCanonicalName();
                    if (name != null && !unsupported.contains(name)) {
                        unsupported.add(name);
                    }
                }
            }
            Class<?> returnType = method.getReturnType();
            if (returnType != void.class && !isTypeSupported(returnType, mappers)) {
                String name = returnType.getCanonicalName();
                if (name != null && !unsupported.contains(name)) {
                    unsupported.add(name);
                }
            }
        }
        return unsupported;
    }

    private static boolean isTypeSupported(Class<?> type, Map<String, Mapper> mappers) {
        if (type == null || type == void.class) {
            return true;
        }
        if (type.isPrimitive()) {
            return true;
        }
        if (type == String.class) {
            return true;
        }
        if (isWrapperType(type)) {
            return true;
        }
        if (isCommonCollectionInterface(type)) {
            return true;
        }
        if (type.isArray()) {
            Class<?> component = type.getComponentType();
            if (component.isPrimitive() || component == String.class) {
                return true;
            }
            return java.io.Serializable.class.isAssignableFrom(component)
                || (mappers != null && mappers.containsKey(component.getCanonicalName()));
        }
        return java.io.Serializable.class.isAssignableFrom(type)
            || (mappers != null && mappers.containsKey(type.getCanonicalName()));
    }

    private static boolean isWrapperType(Class<?> clazz) {
        return clazz == Integer.class || clazz == Long.class || clazz == Double.class
            || clazz == Float.class || clazz == Boolean.class || clazz == Byte.class
            || clazz == Short.class || clazz == Character.class;
    }

    private static boolean isCommonCollectionInterface(Class<?> clazz) {
        return clazz == java.util.List.class || clazz == java.util.Map.class
            || clazz == java.util.Set.class || clazz == java.util.Queue.class
            || clazz == java.util.Deque.class || clazz == java.util.Collection.class;
    }

    /** Sanitize string for logging to prevent log injection (newlines/control chars). */
    private static String sanitizeForLog(String s) {
        if (s == null) return "null";
        String t = s.replace("\r", " ").replace("\n", " ");
        return t.length() > 500 ? t.substring(0, 500) + "..." : t;
    }

}
