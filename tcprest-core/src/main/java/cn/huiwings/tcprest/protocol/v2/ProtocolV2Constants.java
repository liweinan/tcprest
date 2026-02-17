package cn.huiwings.tcprest.protocol.v2;

/**
 * Constants for Protocol v2 format.
 *
 * <p>Protocol v2 format:</p>
 * <pre>
 * Request:  V2|COMPRESSION|ClassName/methodName(TYPE_SIGNATURE)(PARAMS)
 * Response: V2|COMPRESSION|STATUS|BODY
 * </pre>
 *
 * <p>Examples:</p>
 * <pre>
 * V2|0|Calculator/add(II)({{MQ==}}:::{{Mg==}})
 * V2|0|Service/process(Ljava/lang/String;Z)({{aGVsbG8=}}:::{{dHJ1ZQ==}})
 * V2|0|0|{{base64_result}}
 * V2|0|1|{{base64_error_message}}
 * </pre>
 *
 * @since 1.1.0
 */
public final class ProtocolV2Constants {

    /**
     * Protocol version prefix: "V2|"
     */
    public static final String PREFIX = "V2|";

    /**
     * Main separator between protocol parts: "|"
     */
    public static final String SEPARATOR = "|";

    /**
     * Separator between class name and method: "/"
     */
    public static final String CLASS_METHOD_SEPARATOR = "/";

    /**
     * Opening parenthesis for type signature: "("
     */
    public static final String SIGNATURE_START = "(";

    /**
     * Closing parenthesis for type signature: ")"
     */
    public static final String SIGNATURE_END = ")";

    /**
     * Opening delimiter for parameters: "("
     */
    public static final String PARAMS_START = "(";

    /**
     * Closing delimiter for parameters: ")"
     */
    public static final String PARAMS_END = ")";

    /**
     * Parameter separator: ":::"
     */
    public static final String PARAM_SEPARATOR = ":::";

    /**
     * Base64 parameter wrapper start: "{{"
     */
    public static final String PARAM_WRAPPER_START = "{{";

    /**
     * Base64 parameter wrapper end: "}}"
     */
    public static final String PARAM_WRAPPER_END = "}}";

    /**
     * Index of protocol version in request array (after split by SEPARATOR)
     */
    public static final int REQUEST_VERSION_INDEX = 0;

    /**
     * Index of compression flag in request array
     */
    public static final int REQUEST_COMPRESSION_INDEX = 1;

    /**
     * Index of method call in request array
     */
    public static final int REQUEST_METHOD_CALL_INDEX = 2;

    /**
     * Index of compression flag in response array
     */
    public static final int RESPONSE_COMPRESSION_INDEX = 1;

    /**
     * Index of status code in response array
     */
    public static final int RESPONSE_STATUS_INDEX = 2;

    /**
     * Index of body in response array
     */
    public static final int RESPONSE_BODY_INDEX = 3;

    /**
     * Minimum number of parts in a valid v2 request
     */
    public static final int MIN_REQUEST_PARTS = 3;

    /**
     * Minimum number of parts in a valid v2 response
     */
    public static final int MIN_RESPONSE_PARTS = 4;

    private ProtocolV2Constants() {
        // Utility class, prevent instantiation
    }
}
