package cn.huiwings.tcprest.client;

import cn.huiwings.tcprest.annotations.TimeoutAnnotationHandler;
import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.codec.v2.ProtocolV2Codec;
import java.util.logging.Logger;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.MapperHelper;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.ssl.SSLParams;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Protocol V2 TcpRest Client Proxy.
 *
 * <p>Generates dynamic proxy clients for remote TcpRest services using Protocol V2.</p>
 *
 * <p><b>Protocol V2 Features:</b></p>
 * <ul>
 *   <li>Method signature support (enables overloading)</li>
 *   <li>Status codes (SUCCESS, BUSINESS_EXCEPTION, SERVER_ERROR, PROTOCOL_ERROR)</li>
 *   <li>Intelligent type mapping (auto-serialization, collection support)</li>
 *   <li>Security features (checksum, class whitelist)</li>
 * </ul>
 *
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class TcpRestClientProxy implements InvocationHandler {

    private Logger logger = Logger.getLogger(TcpRestClientProxy.class.getName());
    private TcpRestClient tcpRestClient;
    private Map<String, Mapper> mappers;
    private ProtocolV2Codec codec;
    private CompressionConfig compressionConfig = new CompressionConfig(); // Default: disabled
    private SecurityConfig securityConfig = new SecurityConfig(); // Default: no security

    /**
     * Create client proxy with full configuration.
     *
     * @param delegatedClassName target service class name
     * @param host server host
     * @param port server port
     * @param extraMappers custom mappers (optional)
     * @param sslParams SSL configuration (optional)
     * @param compressionConfig compression configuration (optional)
     * @param securityConfig security configuration (optional)
     */
    public TcpRestClientProxy(String delegatedClassName, String host, int port,
                              Map<String, Mapper> extraMappers, SSLParams sslParams,
                              CompressionConfig compressionConfig, SecurityConfig securityConfig) {
        // Create a new HashMap to avoid polluting the static DEFAULT_MAPPERS
        mappers = new HashMap<>(MapperHelper.DEFAULT_MAPPERS);

        if (extraMappers != null) {
            mappers.putAll(extraMappers);
        }

        if (compressionConfig != null) {
            this.compressionConfig = compressionConfig;
        }

        if (securityConfig != null) {
            this.securityConfig = securityConfig;
        }

        // Initialize Protocol V2 codec with security config and mappers
        this.codec = new ProtocolV2Codec(this.securityConfig, this.mappers);

        tcpRestClient = new DefaultTcpRestClient(sslParams, delegatedClassName, host, port);
    }

    /**
     * Create client proxy with compression.
     *
     * @param delegatedClassName target service class name
     * @param host server host
     * @param port server port
     * @param extraMappers custom mappers (optional)
     * @param sslParams SSL configuration (optional)
     * @param compressionConfig compression configuration (optional)
     */
    public TcpRestClientProxy(String delegatedClassName, String host, int port,
                              Map<String, Mapper> extraMappers, SSLParams sslParams,
                              CompressionConfig compressionConfig) {
        this(delegatedClassName, host, port, extraMappers, sslParams, compressionConfig, null);
    }

    /**
     * Create client proxy with SSL.
     *
     * @param delegatedClassName target service class name
     * @param host server host
     * @param port server port
     * @param extraMappers custom mappers (optional)
     * @param sslParams SSL configuration (optional)
     */
    public TcpRestClientProxy(String delegatedClassName, String host, int port,
                              Map<String, Mapper> extraMappers, SSLParams sslParams) {
        this(delegatedClassName, host, port, extraMappers, sslParams, null, null);
    }

    /**
     * Create basic client proxy.
     *
     * @param delegatedClassName target service class name
     * @param host server host
     * @param port server port
     */
    public TcpRestClientProxy(String delegatedClassName, String host, int port) {
        this(delegatedClassName, host, port, null, null, null, null);
    }

    /**
     * Create client proxy with SSL (no custom mappers).
     *
     * @param delegatedClassName target service class name
     * @param host server host
     * @param port server port
     * @param sslParams SSL configuration
     */
    public TcpRestClientProxy(String delegatedClassName, String host, int port, SSLParams sslParams) {
        this(delegatedClassName, host, port, null, sslParams, null, null);
    }

    public void setMappers(Map<String, Mapper> mappers) {
        this.mappers = mappers;
    }

    public Map<String, Mapper> getMappers() {
        return mappers;
    }

    /**
     * Sets security configuration.
     *
     * @param securityConfig security configuration
     */
    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig != null ? securityConfig : new SecurityConfig();
        codec.setSecurityConfig(this.securityConfig);
    }

    /**
     * Gets security configuration.
     *
     * @return security configuration
     */
    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    /**
     * Invoke remote method using Protocol V2.
     *
     * @param proxy the proxy instance
     * @param method the method to invoke
     * @param params method parameters
     * @return method result
     * @throws Throwable if invocation fails
     */
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        String className = method.getDeclaringClass().getCanonicalName();
        if (!className.equals(tcpRestClient.getDeletgatedClassName())) {
            throw new IllegalAccessException("Method cannot be invoked: " + method.getName());
        }

        try {
            // Encode request with v2 format (includes method signature and mappers)
            // V2 supports intelligent type mapping: custom mappers > auto serialization > built-in
            String request = codec.encode(method.getDeclaringClass(), method, params, mappers);

            logger.fine("V2 request: " + request);

            // Send request
            String response = tcpRestClient.sendRequest(request, TimeoutAnnotationHandler.getTimeout(method));
            logger.fine("V2 response: " + response);

            // Decode response (handles status codes and exceptions)
            return codec.decode(response, method.getReturnType());

        } catch (Exception e) {
            // Check if it's a SocketTimeoutException and wrap it as unchecked TimeoutException
            // to avoid dynamic proxy wrapping it as UndeclaredThrowableException
            if (e instanceof java.net.SocketTimeoutException) {
                throw new cn.huiwings.tcprest.exception.TimeoutException(
                    "Request timeout after " + TimeoutAnnotationHandler.getTimeout(method) + " seconds", e);
            }
            // Re-throw other exceptions as-is
            throw e;
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
        logger.info("Client compression configured: " + compressionConfig);
    }

    /**
     * Enable compression with default settings.
     */
    public void enableCompression() {
        this.compressionConfig.setEnabled(true);
        logger.info("Client compression enabled");
    }

    /**
     * Disable compression.
     */
    public void disableCompression() {
        this.compressionConfig.setEnabled(false);
        logger.info("Client compression disabled");
    }

    /**
     * Get the Protocol V2 codec.
     *
     * @return codec instance
     */
    public ProtocolV2Codec getCodec() {
        return codec;
    }
}
