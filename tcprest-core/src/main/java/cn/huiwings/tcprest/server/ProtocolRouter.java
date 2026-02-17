package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.compression.CompressionUtil;
import cn.huiwings.tcprest.conveter.Converter;
import cn.huiwings.tcprest.converter.v2.ProtocolV2Converter;
import cn.huiwings.tcprest.exception.BusinessException;
import cn.huiwings.tcprest.exception.ProtocolException;
import cn.huiwings.tcprest.extractor.Extractor;
import cn.huiwings.tcprest.extractor.v2.ProtocolV2Extractor;
import cn.huiwings.tcprest.invoker.Invoker;
import cn.huiwings.tcprest.invoker.v2.ProtocolV2Invoker;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.protocol.ProtocolVersion;
import cn.huiwings.tcprest.protocol.v2.ProtocolV2Constants;
import cn.huiwings.tcprest.protocol.v2.StatusCode;

import java.io.IOException;
import java.util.Map;

/**
 * Router for protocol version detection and request processing.
 *
 * <p>The ProtocolRouter detects the protocol version from the request
 * and routes it to the appropriate handler (v1 or v2).</p>
 *
 * <p><b>Version Detection:</b></p>
 * <ul>
 *   <li>If request starts with "V2|" → Protocol v2</li>
 *   <li>Otherwise → Protocol v1 (backward compatible)</li>
 * </ul>
 *
 * <p><b>Server Configuration:</b></p>
 * <ul>
 *   <li><b>AUTO</b> (default): Accepts both v1 and v2</li>
 *   <li><b>V1</b>: Only accepts v1 requests</li>
 *   <li><b>V2</b>: Only accepts v2 requests</li>
 * </ul>
 *
 * @since 1.1.0
 */
public class ProtocolRouter {

    private final Logger logger;
    private final ProtocolVersion serverVersion;

    // V1 components (from AbstractTcpRestServer)
    private final Extractor v1Extractor;
    private final Invoker v1Invoker;
    private final Map<String, Mapper> mappers;
    private final CompressionConfig compressionConfig;

    // V2 components
    private final ProtocolV2Extractor v2Extractor;
    private final ProtocolV2Invoker v2Invoker;
    private final ProtocolV2Converter v2Converter;

    /**
     * Create protocol router.
     *
     * @param serverVersion server protocol version (V1, V2, or AUTO)
     * @param v1Extractor v1 extractor
     * @param v1Invoker v1 invoker
     * @param mappers mapper registry for v1
     * @param compressionConfig compression configuration
     * @param logger logger instance
     */
    public ProtocolRouter(
            ProtocolVersion serverVersion,
            Extractor v1Extractor,
            Invoker v1Invoker,
            Map<String, Mapper> mappers,
            CompressionConfig compressionConfig,
            Logger logger) {
        this.serverVersion = serverVersion != null ? serverVersion : ProtocolVersion.AUTO;
        this.v1Extractor = v1Extractor;
        this.v1Invoker = v1Invoker;
        this.mappers = mappers;
        this.compressionConfig = compressionConfig;
        this.logger = logger;

        // Initialize v2 components
        this.v2Extractor = new ProtocolV2Extractor();
        this.v2Invoker = new ProtocolV2Invoker();
        this.v2Converter = new ProtocolV2Converter();
    }

    /**
     * Process request with automatic version detection.
     *
     * @param request the request string
     * @param resourceRegister resource register for v1
     * @return response string
     */
    public String processRequest(String request, ResourceRegister resourceRegister) {
        if (request == null || request.isEmpty()) {
            return handleError(new ProtocolException("Empty request"), ProtocolVersion.V1);
        }

        // Detect protocol version from request
        ProtocolVersion requestVersion = detectVersion(request);

        // Validate server supports this version
        if (!isVersionSupported(requestVersion)) {
            return handleError(
                new ProtocolException("Server does not support " + requestVersion +
                                     " (server version: " + serverVersion + ")"),
                requestVersion
            );
        }

        // Route to appropriate handler
        if (requestVersion == ProtocolVersion.V2) {
            return processV2Request(request, resourceRegister);
        } else {
            return processV1Request(request, resourceRegister);
        }
    }

    /**
     * Detect protocol version from request.
     *
     * @param request the request string
     * @return detected version (V1 or V2)
     */
    private ProtocolVersion detectVersion(String request) {
        if (request.startsWith(ProtocolV2Constants.PREFIX)) {
            return ProtocolVersion.V2;
        } else {
            return ProtocolVersion.V1;
        }
    }

    /**
     * Check if server supports the requested version.
     *
     * @param requestVersion the requested version
     * @return true if supported
     */
    private boolean isVersionSupported(ProtocolVersion requestVersion) {
        if (serverVersion == ProtocolVersion.AUTO) {
            return true; // AUTO accepts both
        }
        return serverVersion == requestVersion;
    }

    /**
     * Process v2 request with exception handling.
     *
     * @param request the v2 request
     * @param resourceRegister resource register
     * @return v2 response with status code
     */
    private String processV2Request(String request, ResourceRegister resourceRegister) {
        try {
            // Extract context
            Context context = v2Extractor.extract(request);

            // Get or create resource instance
            Class<?> targetClass = context.getTargetClass();
            Object instance = resourceRegister.getResource(targetClass.getName());
            if (instance == null) {
                instance = v2Invoker.createInstance(targetClass);
            }
            context.setTargetInstance(instance);

            // Invoke method
            Object result = v2Invoker.invoke(context);

            // Encode success response
            return v2Converter.encodeResponse(result, StatusCode.SUCCESS);

        } catch (BusinessException e) {
            // Business exception - expected error
            logger.warn("Business exception: " + e.getMessage());
            return v2Converter.encodeException(e, StatusCode.BUSINESS_EXCEPTION);

        } catch (ProtocolException e) {
            // Protocol error - malformed request
            logger.error("Protocol error: " + e.getMessage());
            return v2Converter.encodeException(e, StatusCode.PROTOCOL_ERROR);

        } catch (Exception e) {
            // Server error - unexpected exception
            logger.error("Server error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return v2Converter.encodeException(e, StatusCode.SERVER_ERROR);
        }
    }

    /**
     * Process v1 request (legacy behavior with compression support).
     *
     * @param request the v1 request
     * @param resourceRegister resource register
     * @return v1 response
     */
    private String processV1Request(String request, ResourceRegister resourceRegister) {
        try {
            // Decompress request if needed
            String decompressedRequest = request;
            if (request != null && (request.startsWith("0|") || request.startsWith("1|"))) {
                try {
                    decompressedRequest = CompressionUtil.decompress(request);
                    if (CompressionUtil.isCompressed(request)) {
                        logger.debug("Decompressed incoming request");
                    }
                } catch (IOException e) {
                    logger.error("Failed to decompress request: " + e.getMessage());
                    decompressedRequest = request;
                }
            }

            // Extract calling class and method from request
            Context context = v1Extractor.extract(decompressedRequest);

            // Get singleton resource instance (v1 invoker will create if null)
            Class<?> targetClass = context.getTargetClass();
            Object instance = resourceRegister.getResource(targetClass.getName());
            context.setTargetInstance(instance);

            // Invoke method (v1 invoker swallows exceptions and returns NullObj)
            Object responseObject = v1Invoker.invoke(context);
            logger.debug("***responseObject: " + responseObject);

            // Get returned object and encode it to string response
            Mapper responseMapper = context.getConverter().getMapper(mappers, responseObject.getClass());
            String response = context.getConverter().encodeParam(responseMapper.objectToString(responseObject));

            // Compress response if enabled
            if (compressionConfig.isEnabled()) {
                try {
                    String compressed = CompressionUtil.compress(response, compressionConfig);
                    if (CompressionUtil.isCompressed(compressed)) {
                        double ratio = CompressionUtil.getCompressionRatio(response, compressed);
                        logger.debug("Compressed response (saved " + String.format("%.1f", ratio) + "%)");
                    }
                    return compressed;
                } catch (IOException e) {
                    logger.error("Failed to compress response: " + e.getMessage());
                    return "0|" + response; // Fallback with uncompressed prefix
                }
            } else {
                // Add prefix for protocol compatibility
                return "0|" + response;
            }

        } catch (Exception e) {
            logger.error("V1 request processing error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return "0|"; // V1 returns empty with prefix on error
        }
    }

    /**
     * Handle errors and return appropriate error response.
     *
     * @param error the error
     * @param version the protocol version
     * @return error response
     */
    private String handleError(Exception error, ProtocolVersion version) {
        if (version == ProtocolVersion.V2) {
            return v2Converter.encodeException(error, StatusCode.PROTOCOL_ERROR);
        } else {
            return ""; // V1 returns empty on error
        }
    }

    /**
     * Get server protocol version.
     *
     * @return server version
     */
    public ProtocolVersion getServerVersion() {
        return serverVersion;
    }
}
