package cn.huiwings.tcprest.protocol.v2;

/**
 * Constants for Protocol v2 format.
 *
 * <p><b>Protocol v2 Simplified Format (2026-02-19):</b></p>
 * <pre>
 * Request:  V2|COMPRESSION|{{base64(META)}}|[param1,param2,param3]
 * Response: V2|COMPRESSION|STATUS|{{base64(BODY)}}
 * </pre>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>JSON-style array format for parameters: [p1,p2,p3]</li>
 *   <li>Single-layer Base64 encoding for parameters (no double encoding)</li>
 *   <li>Cleaner, more readable protocol</li>
 *   <li>Easier to parse and debug</li>
 * </ul>
 *
 * <p><b>Examples:</b></p>
 * <pre>
 * V2|0|{{Y2FsYy9hZGQoSUkp}}|[MQ==,Mg==]
 * V2|0|{{c2VydmljZS9wcm9jZXNzKExqYXZhL2xhbmcvU3RyaW5nO1op}}|[aGVsbG8=,dHJ1ZQ==]
 * V2|0|0|{{NDI=}}
 * V2|0|1|{{RXJyb3I6IGludmFsaWQgaW5wdXQ=}}
 * </pre>
 *
 * @since 1.1.0
 * @version 2.0 (2026-02-19) - Simplified format with JSON-style arrays
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
     * Opening delimiter for parameter array: "["
     */
    public static final String PARAMS_ARRAY_START = "[";

    /**
     * Closing delimiter for parameter array: "]"
     */
    public static final String PARAMS_ARRAY_END = "]";

    /**
     * Parameter separator in array: ","
     */
    public static final String PARAM_SEPARATOR = ",";

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
