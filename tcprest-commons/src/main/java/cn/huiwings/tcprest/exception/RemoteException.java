package cn.huiwings.tcprest.exception;

/**
 * Base exception for remote exceptions that cannot be reconstructed on the client side.
 *
 * <p>When a server throws an exception whose class is not available on the client,
 * TcpRest wraps it in a RemoteException subclass that preserves the original exception
 * type information while allowing the client to handle it appropriately.</p>
 *
 * <p><b>Subclasses:</b></p>
 * <ul>
 *   <li>{@link RemoteBusinessException} - Server business logic errors</li>
 *   <li>{@link RemoteServerException} - Server internal errors</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * try {
 *     client.placeOrder(order);
 * } catch (RemoteBusinessException e) {
 *     // Business error - can retry with corrected input
 *     logger.warn("Business error: " + e.getRemoteExceptionType() + ": " + e.getMessage());
 * } catch (RemoteServerException e) {
 *     // Server error - should alert, cannot retry
 *     logger.error("Server error: " + e.getRemoteExceptionType() + ": " + e.getMessage());
 * }
 * </pre>
 *
 * @since 1.1.0
 * @author Weinan Li
 */
public abstract class RemoteException extends RuntimeException {

    private final String remoteExceptionType;

    /**
     * Constructs a remote exception.
     *
     * @param remoteExceptionType the fully qualified class name of the original exception
     * @param message the exception message
     */
    public RemoteException(String remoteExceptionType, String message) {
        super(formatMessage(remoteExceptionType, message));
        this.remoteExceptionType = remoteExceptionType;
    }

    /**
     * Constructs a remote exception with a cause.
     *
     * @param remoteExceptionType the fully qualified class name of the original exception
     * @param message the exception message
     * @param cause the cause
     */
    public RemoteException(String remoteExceptionType, String message, Throwable cause) {
        super(formatMessage(remoteExceptionType, message), cause);
        this.remoteExceptionType = remoteExceptionType;
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
            // Extract simple class name for readability
            String simpleName = type.contains(".")
                ? type.substring(type.lastIndexOf('.') + 1)
                : type;
            return simpleName + ": " + (message != null ? message : "");
        }
        return message != null ? message : "Remote invocation failed";
    }

    /**
     * Get the fully qualified class name of the remote exception.
     *
     * @return exception type (e.g., "com.example.OrderValidationException")
     */
    public String getRemoteExceptionType() {
        return remoteExceptionType;
    }

    /**
     * Check if this represents a business exception.
     *
     * @return true if business exception, false if server error
     */
    public abstract boolean isBusinessException();

    /**
     * Check if this represents a server error.
     *
     * @return true if server error, false if business exception
     */
    public boolean isServerError() {
        return !isBusinessException();
    }
}
