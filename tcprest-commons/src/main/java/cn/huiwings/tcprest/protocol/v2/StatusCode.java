package cn.huiwings.tcprest.protocol.v2;

/**
 * Status codes for Protocol v2 responses.
 *
 * <p>Status codes enable proper exception propagation from server to client:</p>
 * <ul>
 *   <li><b>SUCCESS (0)</b>: Method executed successfully, body contains result</li>
 *   <li><b>BUSINESS_EXCEPTION (1)</b>: Business logic error (e.g., validation failure)</li>
 *   <li><b>SERVER_ERROR (2)</b>: Internal server error (e.g., NullPointerException)</li>
 *   <li><b>PROTOCOL_ERROR (3)</b>: Protocol parsing error (e.g., malformed request)</li>
 * </ul>
 *
 * @since 1.1.0
 */
public enum StatusCode {
    /**
     * Success - method executed successfully.
     * Response body contains the serialized result.
     */
    SUCCESS(0),

    /**
     * Business exception - application logic error.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>Validation failures (invalid input)</li>
     *   <li>Business rule violations</li>
     *   <li>Expected error conditions</li>
     * </ul>
     *
     * <p>Response body contains exception type and message.</p>
     */
    BUSINESS_EXCEPTION(1),

    /**
     * Server error - unexpected internal error.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>NullPointerException</li>
     *   <li>IllegalStateException</li>
     *   <li>Database connection failures</li>
     * </ul>
     *
     * <p>Response body contains exception type and message.</p>
     */
    SERVER_ERROR(2),

    /**
     * Protocol error - request parsing failure.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>Malformed request format</li>
     *   <li>Invalid type signature</li>
     *   <li>Unknown method or class</li>
     * </ul>
     *
     * <p>Response body contains error details.</p>
     */
    PROTOCOL_ERROR(3);

    private final int code;

    StatusCode(int code) {
        this.code = code;
    }

    /**
     * Get the numeric status code.
     *
     * @return status code (0-3)
     */
    public int getCode() {
        return code;
    }

    /**
     * Convert numeric code to StatusCode enum.
     *
     * @param code numeric status code
     * @return corresponding StatusCode
     * @throws IllegalArgumentException if code is invalid
     */
    public static StatusCode fromCode(int code) {
        for (StatusCode status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid status code: " + code);
    }

    /**
     * Convert string code to StatusCode enum.
     *
     * @param code string representation of status code
     * @return corresponding StatusCode
     * @throws IllegalArgumentException if code is invalid
     */
    public static StatusCode fromString(String code) {
        try {
            return fromCode(Integer.parseInt(code));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid status code: " + code, e);
        }
    }
}
