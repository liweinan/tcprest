package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.codec.ProtocolCodec;
import cn.huiwings.tcprest.codec.v2.ProtocolV2Codec;
import cn.huiwings.tcprest.exception.BusinessException;
import cn.huiwings.tcprest.exception.ProtocolException;
import cn.huiwings.tcprest.parser.RequestParser;
import cn.huiwings.tcprest.parser.v2.ProtocolV2Parser;
import cn.huiwings.tcprest.invoker.v2.ProtocolV2Invoker;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.protocol.v2.ProtocolV2Constants;
import cn.huiwings.tcprest.protocol.v2.StatusCode;

import java.util.Map;

/**
 * Protocol V2 request router and processor.
 *
 * <p>The ProtocolRouter handles all Protocol V2 requests with comprehensive
 * exception handling and status code support.</p>
 *
 * <p><b>Protocol V2 Features:</b></p>
 * <ul>
 *   <li>Method signature support (enables overloading)</li>
 *   <li>Status codes (SUCCESS, BUSINESS_EXCEPTION, SERVER_ERROR, PROTOCOL_ERROR)</li>
 *   <li>Intelligent type mapping (auto-serialization, collection support)</li>
 *   <li>Security features (checksum, class whitelist)</li>
 * </ul>
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
 * @since 1.1.0 (V1 support removed in version 2.0.0)
 */
public class ProtocolRouter {

    private final Logger logger;
    private final RequestParser parser;
    private final ProtocolV2Invoker invoker;
    private final ProtocolCodec codec;

    /**
     * Create protocol router with V2 components.
     *
     * @param mappers mapper registry for parameter serialization
     * @param logger logger instance
     */
    public ProtocolRouter(Map<String, Mapper> mappers, Logger logger) {
        this.logger = logger;
        this.parser = new ProtocolV2Parser(mappers);
        this.invoker = new ProtocolV2Invoker();
        this.codec = new ProtocolV2Codec(mappers);
    }

    /**
     * Set security configuration for protocol components.
     *
     * @param securityConfig security configuration
     */
    public void setSecurityConfig(cn.huiwings.tcprest.security.SecurityConfig securityConfig) {
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
     * Process Protocol V2 request.
     *
     * @param request the V2 request string
     * @param resourceRegister resource register for finding service instances
     * @return V2 response string with status code
     */
    public String processRequest(String request, ResourceRegister resourceRegister) {
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
            Object instance = ResourceResolver.findResourceInstance(targetClass, resourceRegister, logger);
            context.setTargetInstance(instance);

            // Invoke method
            Object result = invoker.invoke(context);

            // Encode success response
            return ((ProtocolV2Codec) codec).encodeResponse(result, StatusCode.SUCCESS);

        } catch (BusinessException e) {
            // Business exception - expected error from business logic
            logger.warn("Business exception: " + e.getMessage());
            return ((ProtocolV2Codec) codec).encodeException(e, StatusCode.BUSINESS_EXCEPTION);

        } catch (ProtocolException e) {
            // Protocol error - malformed request or parsing failure
            logger.error("Protocol error: " + e.getMessage());
            return ((ProtocolV2Codec) codec).encodeException(e, StatusCode.PROTOCOL_ERROR);

        } catch (Exception e) {
            // Server error - unexpected exception during processing
            logger.error("Server error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
