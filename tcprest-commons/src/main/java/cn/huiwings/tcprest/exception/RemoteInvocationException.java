package cn.huiwings.tcprest.exception;

/**
 * Exception thrown on the client side when a remote method invocation fails.
 *
 * <p>This exception wraps server-side exceptions and propagates them to the
 * client, preserving the exception type and message.</p>
 *
 * <p>RemoteInvocationException is used for two categories of server errors:</p>
 * <ul>
 *   <li><b>Business Exceptions</b>: Application logic errors (validation failures, etc.)</li>
 *   <li><b>Server Errors</b>: Unexpected internal errors (NullPointerException, etc.)</li>
 * </ul>
 *
 * <p><b>Usage on Client:</b></p>
 * <pre>
 * try {
 *     int result = calculator.divide(10, 0);
 * } catch (RemoteInvocationException e) {
 *     if (e.isBusinessException()) {
 *         // Handle expected business error
 *         System.err.println("Business error: " + e.getMessage());
 *     } else {
 *         // Handle unexpected server error
 *         System.err.println("Server error: " + e.getRemoteExceptionType());
 *     }
 * }
 * </pre>
 *
 * @since 1.1.0
 */
public class RemoteInvocationException extends RuntimeException {

    private final String remoteExceptionType;
    private final boolean businessException;

    /**
     * Constructs a remote invocation exception for a business exception.
     *
     * @param remoteExceptionType the type of the remote exception (e.g., "ValidationException")
     * @param message the exception message
     */
    public RemoteInvocationException(String remoteExceptionType, String message) {
        this(remoteExceptionType, message, true);
    }

    /**
     * Constructs a remote invocation exception.
     *
     * @param remoteExceptionType the type of the remote exception
     * @param message the exception message
     * @param businessException true if this is a business exception, false for server error
     */
    public RemoteInvocationException(String remoteExceptionType, String message, boolean businessException) {
        super(formatMessage(remoteExceptionType, message));
        this.remoteExceptionType = remoteExceptionType;
        this.businessException = businessException;
    }

    /**
     * Constructs a remote invocation exception with a cause.
     *
     * @param remoteExceptionType the type of the remote exception
     * @param message the exception message
     * @param businessException true if this is a business exception
     * @param cause the cause
     */
    public RemoteInvocationException(String remoteExceptionType, String message, boolean businessException, Throwable cause) {
        super(formatMessage(remoteExceptionType, message), cause);
        this.remoteExceptionType = remoteExceptionType;
        this.businessException = businessException;
    }

    /**
     * Format exception message with remote type information.
     *
     * @param type the exception type
     * @param message the exception message
     * @return formatted message
     */
    private static String formatMessage(String type, String message) {
        if (type != null && !type.isEmpty()) {
            return type + ": " + (message != null ? message : "");
        }
        return message != null ? message : "Remote invocation failed";
    }

    /**
     * Get the type of the remote exception.
     *
     * @return exception type (e.g., "ValidationException", "NullPointerException")
     */
    public String getRemoteExceptionType() {
        return remoteExceptionType;
    }

    /**
     * Check if this represents a business exception.
     *
     * @return true if business exception, false if server error
     */
    public boolean isBusinessException() {
        return businessException;
    }

    /**
     * Check if this represents a server error.
     *
     * @return true if server error, false if business exception
     */
    public boolean isServerError() {
        return !businessException;
    }
}
