package cn.huiwings.tcprest.exception;

/**
 * Exception thrown when protocol parsing or validation fails.
 *
 * <p>This exception indicates problems with the protocol format itself,
 * such as:</p>
 * <ul>
 *   <li>Malformed request format</li>
 *   <li>Invalid type signature</li>
 *   <li>Version mismatch</li>
 *   <li>Unknown method or class</li>
 *   <li>Parameter count mismatch</li>
 * </ul>
 *
 * <p>ProtocolException maps to StatusCode.PROTOCOL_ERROR (3) in Protocol v2.</p>
 *
 * @since 1.1.0
 */
public class ProtocolException extends RuntimeException {

    /**
     * Constructs a new protocol exception with the specified detail message.
     *
     * @param message the detail message
     */
    public ProtocolException(String message) {
        super(message);
    }

    /**
     * Constructs a new protocol exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new protocol exception with the specified cause.
     *
     * @param cause the cause
     */
    public ProtocolException(Throwable cause) {
        super(cause);
    }
}
