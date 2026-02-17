package cn.huiwings.tcprest.client;

import cn.huiwings.tcprest.annotations.TimeoutAnnotationHandler;
import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.compression.CompressionUtil;
import cn.huiwings.tcprest.conveter.Converter;
import cn.huiwings.tcprest.conveter.DefaultConverter;
import cn.huiwings.tcprest.converter.v2.ProtocolV2Converter;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.MapperHelper;
import cn.huiwings.tcprest.protocol.NullObj;
import cn.huiwings.tcprest.protocol.ProtocolVersion;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.ssl.SSLParam;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * TcpRestClientProxy can generate a client from resource class/interface
 *
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class TcpRestClientProxy implements InvocationHandler {

    private Logger logger = LoggerFactory.getDefaultLogger();

    private TcpRestClient tcpRestClient;

    private Map<String, Mapper> mappers;

    private Converter converter = new DefaultConverter();

    private ProtocolV2Converter v2Converter = new ProtocolV2Converter();

    private CompressionConfig compressionConfig = new CompressionConfig(); // Default: disabled

    private ProtocolConfig protocolConfig = new ProtocolConfig(); // Default: V1

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, Map<String, Mapper> extraMappers, SSLParam sslParam, CompressionConfig compressionConfig, ProtocolConfig protocolConfig) {
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

        tcpRestClient = new DefaultTcpRestClient(sslParam, deletgatedClassName, host, port);
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, Map<String, Mapper> extraMappers, SSLParam sslParam, CompressionConfig compressionConfig) {
        this(deletgatedClassName, host, port, extraMappers, sslParam, compressionConfig, null);
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, Map<String, Mapper> extraMappers, SSLParam sslParam) {
        this(deletgatedClassName, host, port, extraMappers, sslParam, null, null);
    }

    public void setMappers(Map<String, Mapper> mappers) {
        this.mappers = mappers;
    }

    public Map<String, Mapper> getMappers() {
        return mappers;
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port) {
        this(deletgatedClassName, host, port, null, null, null, null);
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, SSLParam sslParam) {
        this(deletgatedClassName, host, port, null, sslParam, null, null);
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
     * Invoke using Protocol v1 (legacy).
     */
    private Object invokeV1(Method method, Object[] params) throws Throwable {
        String request = converter.encode(method.getDeclaringClass(), method, params, mappers);

        // Compress request if enabled
        String finalRequest = request;
        if (compressionConfig.isEnabled()) {
            try {
                finalRequest = CompressionUtil.compress(request, compressionConfig);
                if (CompressionUtil.isCompressed(finalRequest)) {
                    double ratio = CompressionUtil.getCompressionRatio(request, finalRequest);
                    logger.debug("Compressed request (saved " + String.format("%.1f", ratio) + "%)");
                }
            } catch (IOException e) {
                logger.error("Failed to compress request: " + e.getMessage());
                finalRequest = "0|" + request; // Fallback with uncompressed prefix
            }
        } else {
            // Add prefix for protocol compatibility
            finalRequest = "0|" + request;
        }

        String response = tcpRestClient.sendRequest(finalRequest, TimeoutAnnotationHandler.getTimeout(method));
        logger.debug("response: " + response);

        // Decompress response if needed
        String decompressedResponse = response;
        if (response != null && (response.startsWith("0|") || response.startsWith("1|"))) {
            try {
                decompressedResponse = CompressionUtil.decompress(response);
                if (CompressionUtil.isCompressed(response)) {
                    logger.debug("Decompressed response");
                }
            } catch (IOException e) {
                logger.error("Failed to decompress response: " + e.getMessage());
                decompressedResponse = response; // Fallback to original
            }
        }

        String respStr = converter.decodeParam(decompressedResponse);

        String mapperKey = method.getReturnType().getCanonicalName();
        if (respStr.equals(TcpRestProtocol.NULL))
            mapperKey = NullObj.class.getCanonicalName();

        logger.debug("***TcpRestClientProxy - response: " + respStr);

        Mapper mapper = converter.getMapper(mappers, mapperKey);

        logger.debug("***TcpRestClientProxy - mapper: " + mapper);

        if (mapper == null) {
            throw new IllegalAccessException("***TcpRestClientProxy - mapper cannot be found for response object: " + respStr.toString());
        }

        return mapper.stringToObject(respStr);
    }

    /**
     * Invoke using Protocol v2 (with signatures and status codes).
     */
    private Object invokeV2(Method method, Object[] params) throws Throwable {
        // Encode request with v2 format (includes method signature)
        String request = v2Converter.encode(method.getDeclaringClass(), method, params);

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
