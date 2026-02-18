package cn.huiwings.tcprest.client;

import cn.huiwings.tcprest.annotations.TimeoutAnnotationHandler;
import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.compression.CompressionUtil;
import cn.huiwings.tcprest.converter.Converter;
import cn.huiwings.tcprest.converter.DefaultConverter;
import cn.huiwings.tcprest.converter.v2.ProtocolV2Converter;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.MapperHelper;
import cn.huiwings.tcprest.protocol.NullObj;
import cn.huiwings.tcprest.protocol.ProtocolVersion;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.ssl.SSLParam;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Security-Enhanced TcpRestClientProxy.
 *
 * <p>Generates dynamic proxy clients with protocol security features.</p>
 *
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class TcpRestClientProxy implements InvocationHandler {

    private Logger logger = LoggerFactory.getDefaultLogger();
    private TcpRestClient tcpRestClient;
    private Map<String, Mapper> mappers;
    private Converter converter;
    private ProtocolV2Converter v2Converter;
    private CompressionConfig compressionConfig = new CompressionConfig(); // Default: disabled
    private ProtocolConfig protocolConfig = new ProtocolConfig(); // Default: V1
    private SecurityConfig securityConfig = new SecurityConfig(); // Default: no security

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, Map<String, Mapper> extraMappers, SSLParam sslParam, CompressionConfig compressionConfig, ProtocolConfig protocolConfig) {
        this(deletgatedClassName, host, port, extraMappers, sslParam, compressionConfig, protocolConfig, null);
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, Map<String, Mapper> extraMappers, SSLParam sslParam, CompressionConfig compressionConfig, ProtocolConfig protocolConfig, SecurityConfig securityConfig) {
        mappers = MapperHelper.DEFAULT_MAPPERS;

        if (extraMappers != null) {
            mappers.putAll(extraMappers);
        }

        if (compressionConfig != null) {
            this.compressionConfig = compressionConfig;
        }

        if (protocolConfig != null) {
            this.protocolConfig = protocolConfig;
        }

        if (securityConfig != null) {
            this.securityConfig = securityConfig;
        }

        // Initialize converters with security config
        this.converter = new DefaultConverter(this.securityConfig);
        this.v2Converter = new ProtocolV2Converter(this.securityConfig);

        tcpRestClient = new DefaultTcpRestClient(sslParam, deletgatedClassName, host, port);
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, Map<String, Mapper> extraMappers, SSLParam sslParam, CompressionConfig compressionConfig) {
        this(deletgatedClassName, host, port, extraMappers, sslParam, compressionConfig, null, null);
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, Map<String, Mapper> extraMappers, SSLParam sslParam) {
        this(deletgatedClassName, host, port, extraMappers, sslParam, null, null, null);
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port) {
        this(deletgatedClassName, host, port, null, null, null, null, null);
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, SSLParam sslParam) {
        this(deletgatedClassName, host, port, null, sslParam, null, null, null);
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

        // Update converters
        if (converter instanceof DefaultConverter) {
            ((DefaultConverter) converter).setSecurityConfig(this.securityConfig);
        }
        if (v2Converter != null) {
            v2Converter.setSecurityConfig(this.securityConfig);
        }
    }

    /**
     * Gets security configuration.
     *
     * @return security configuration
     */
    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public Object invoke(Object o, Method method, Object[] params) throws Throwable {
        String className = method.getDeclaringClass().getCanonicalName();
        if (!className.equals(tcpRestClient.getDeletgatedClassName())) {
            throw new IllegalAccessException("***TcpRestClientProxy - method cannot be invoked: " + method.getName());
        }

        // Use v2 converter if protocol is v2
        if (protocolConfig.isV2()) {
            return invokeV2(method, params);
        } else {
            return invokeV1(method, params);
        }
    }

    /**
     * Invoke using Protocol v1 (security-enhanced).
     *
     * <p>New format: {@code 0|META|PARAMS|CHK:value}</p>
     */
    private Object invokeV1(Method method, Object[] params) throws Throwable {
        // Encode request using secure protocol
        // converter.encode() now generates: 0|{{base64(meta)}}|{{base64(params)}}|CHK:value
        String request = converter.encode(method.getDeclaringClass(), method, params, mappers);

        logger.debug("***TcpRestClientProxy - encoded request: " + request);

        // Send request
        String response = tcpRestClient.sendRequest(request, TimeoutAnnotationHandler.getTimeout(method));
        logger.debug("***TcpRestClientProxy - received response: " + response);

        // Parse response format: 0|{{base64(result)}}|CHK:value
        if (response == null || response.isEmpty()) {
            return null;
        }

        // Step 1: Split checksum
        String[] parts = ProtocolSecurity.splitChecksum(response);
        String messageWithoutChecksum = parts[0];
        String checksum = parts[1];

        // Step 2: Verify checksum if present
        if (!checksum.isEmpty()) {
            if (!ProtocolSecurity.verifyChecksum(messageWithoutChecksum, checksum, securityConfig)) {
                throw new cn.huiwings.tcprest.exception.SecurityException(
                    "Response checksum verification failed"
                );
            }
            logger.debug("***TcpRestClientProxy - response checksum verified");
        }

        // Step 3: Split response components
        String[] components = messageWithoutChecksum.split("\\" + TcpRestProtocol.COMPONENT_SEPARATOR, -1);

        if (components.length < 2) {
            // Legacy response format or error - try to decode directly
            logger.warn("***TcpRestClientProxy - unexpected response format, attempting direct decode");
            return decodeResult(response, method);
        }

        String statusOrCompression = components[0];
        String resultEncoded = components[1]; // This is {{base64(result)}}

        // Step 4: Decode result using converter.decodeParam()
        // The result is in {{base64}} format, which is what encodeParam() produces
        String decodedResult = converter.decodeParam(resultEncoded);

        logger.debug("***TcpRestClientProxy - decoded result: " + decodedResult);

        // Step 5: Map to return type
        String mapperKey = method.getReturnType().getCanonicalName();
        if (decodedResult.equals(TcpRestProtocol.NULL)) {
            mapperKey = NullObj.class.getCanonicalName();
        }

        Mapper mapper = converter.getMapper(mappers, mapperKey);

        if (mapper == null) {
            throw new IllegalAccessException("***TcpRestClientProxy - mapper cannot be found for response object: " + decodedResult);
        }

        return mapper.stringToObject(decodedResult);
    }

    /**
     * Decode result with fallback to legacy format.
     */
    private Object decodeResult(String response, Method method) throws Exception {
        String respStr = converter.decodeParam(response);

        String mapperKey = method.getReturnType().getCanonicalName();
        if (respStr.equals(TcpRestProtocol.NULL)) {
            mapperKey = NullObj.class.getCanonicalName();
        }

        Mapper mapper = converter.getMapper(mappers, mapperKey);
        return mapper.stringToObject(respStr);
    }

    /**
     * Invoke using Protocol v2 (with signatures, status codes, and security).
     */
    private Object invokeV2(Method method, Object[] params) throws Throwable {
        // Encode request with v2 format (includes method signature)
        // Note: V2 doesn't use mappers, so we pass null
        String request = v2Converter.encode(method.getDeclaringClass(), method, params, null);

        String response = tcpRestClient.sendRequest(request, TimeoutAnnotationHandler.getTimeout(method));
        logger.debug("V2 response: " + response);

        // Decode response (handles status codes and exceptions)
        return v2Converter.decode(response, method.getReturnType());
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
     * Enable compression with default settings
     */
    public void enableCompression() {
        this.compressionConfig.setEnabled(true);
        logger.info("Client compression enabled");
    }

    /**
     * Disable compression
     */
    public void disableCompression() {
        this.compressionConfig.setEnabled(false);
        logger.info("Client compression disabled");
    }
}
