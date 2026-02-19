package cn.huiwings.tcprest.exception;

/**
 * Exception thrown when a server-side internal error cannot be reconstructed on the client.
 *
 * <p>This exception is used as a fallback when:</p>
 * <ul>
 *   <li>Server throws an unexpected exception (NOT a {@link BusinessException})</li>
 *   <li>Client does not have that exception class in its classpath</li>
 *   <li>TcpRest cannot recreate the original exception type</li>
 * </ul>
 *
 * <p>The client can still identify the error type and message, even though it doesn't have
 * the exact exception class.</p>
 *
 * <p><b>Example Scenario:</b></p>
 * <pre>
 * // Server side:
 * public class CustomDatabaseException extends RuntimeException {
 *     public CustomDatabaseException(String message) {
 *         super(message);
 *     }
 * }
 *
 * public class UserService implements UserServiceInterface {
 *     public User getUser(long id) {
 *         try {
 *             return database.query(...);
 *         } catch (PoolExhaustedException e) {
 *             throw new CustomDatabaseException("Connection pool exhausted");
 *         }
 *     }
 * }
 *
 * // Client side (doesn't have CustomDatabaseException.class):
 * try {
 *     User user = userClient.getUser(123);
 * } catch (RemoteServerException e) {
 *     // Can identify the error type
 *     System.err.println("Server error: " + e.getRemoteExceptionType());
 *     // Output: "Server error: com.example.CustomDatabaseException"
 *     // e.getMessage() returns: "CustomDatabaseException: Connection pool exhausted"
 *
 *     // Should log and alert - this is NOT a business error
 *     logger.error("Unexpected server error", e);
 *     alertOps("Server database issue detected");
 *
 *     // Cannot retry - this is a server-side problem
 * }
 * </pre>
 *
 * <p><b>Semantic Difference from Business Exceptions:</b></p>
 * <ul>
 *   <li>Server exceptions represent unexpected internal errors (bugs, infrastructure issues)</li>
 *   <li>They cannot be resolved by the client - server-side fix required</li>
 *   <li>Should trigger logging/alerting, not user-facing error messages</li>
 *   <li>Contrast with {@link RemoteBusinessException} which represents expected application logic errors</li>
 * </ul>
 *
 * <p><b>Common Server Exception Examples:</b></p>
 * <ul>
 *   <li>NullPointerException, IllegalStateException - programming bugs</li>
 *   <li>DatabaseException, ConnectionException - infrastructure issues</li>
 *   <li>Custom framework exceptions not available on client</li>
 * </ul>
 *
 * @since 1.1.0
 * @author Weinan Li
 * @see RemoteBusinessException
 * @see BusinessException
 */
public class RemoteServerException extends RemoteException {

    /**
     * Constructs a remote server exception.
     *
     * @param remoteExceptionType the fully qualified class name of the original exception
     * @param message the exception message
     */
    public RemoteServerException(String remoteExceptionType, String message) {
        super(remoteExceptionType, message);
    }

    /**
     * Constructs a remote server exception with a cause.
     *
     * @param remoteExceptionType the fully qualified class name of the original exception
     * @param message the exception message
     * @param cause the cause
     */
    public RemoteServerException(String remoteExceptionType, String message, Throwable cause) {
        super(remoteExceptionType, message, cause);
    }

    @Override
    public boolean isBusinessException() {
        return false;
    }
}
