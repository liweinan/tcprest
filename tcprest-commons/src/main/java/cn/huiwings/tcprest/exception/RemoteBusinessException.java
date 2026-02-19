package cn.huiwings.tcprest.exception;

/**
 * Exception thrown when a server-side business exception cannot be reconstructed on the client.
 *
 * <p>This exception is used as a fallback when:</p>
 * <ul>
 *   <li>Server throws a {@link BusinessException} subclass (e.g., OrderValidationException)</li>
 *   <li>Client does not have that exception class in its classpath</li>
 *   <li>TcpRest cannot recreate the original exception type</li>
 * </ul>
 *
 * <p>The client can still handle the error appropriately by checking the remote exception type
 * and message, even though it doesn't have the exact exception class.</p>
 *
 * <p><b>Example Scenario:</b></p>
 * <pre>
 * // Server side:
 * public class OrderValidationException extends BusinessException {
 *     public OrderValidationException(String message) {
 *         super(message);
 *     }
 * }
 *
 * public class OrderService implements OrderServiceInterface {
 *     public void placeOrder(Order order) {
 *         if (order.getAmount() > limit) {
 *             throw new OrderValidationException("Order amount exceeds limit");
 *         }
 *     }
 * }
 *
 * // Client side (doesn't have OrderValidationException.class):
 * try {
 *     orderClient.placeOrder(order);
 * } catch (RemoteBusinessException e) {
 *     // Can still handle it appropriately
 *     System.err.println("Business error from server: " + e.getRemoteExceptionType());
 *     // Output: "Business error from server: com.example.OrderValidationException"
 *     // e.getMessage() returns: "OrderValidationException: Order amount exceeds limit"
 *
 *     // Can retry with corrected input
 *     if (e.getRemoteExceptionType().contains("ValidationException")) {
 *         retryWithCorrectedInput();
 *     }
 * }
 * </pre>
 *
 * <p><b>Semantic Difference from Server Errors:</b></p>
 * <ul>
 *   <li>Business exceptions represent expected error conditions in application logic</li>
 *   <li>They can often be resolved by correcting input or user actions</li>
 *   <li>Contrast with {@link RemoteServerException} which represents unexpected internal errors</li>
 * </ul>
 *
 * @since 1.1.0
 * @author Weinan Li
 * @see RemoteServerException
 * @see BusinessException
 */
public class RemoteBusinessException extends RemoteException {

    /**
     * Constructs a remote business exception.
     *
     * @param remoteExceptionType the fully qualified class name of the original exception
     * @param message the exception message
     */
    public RemoteBusinessException(String remoteExceptionType, String message) {
        super(remoteExceptionType, message);
    }

    /**
     * Constructs a remote business exception with a cause.
     *
     * @param remoteExceptionType the fully qualified class name of the original exception
     * @param message the exception message
     * @param cause the cause
     */
    public RemoteBusinessException(String remoteExceptionType, String message, Throwable cause) {
        super(remoteExceptionType, message, cause);
    }

    @Override
    public boolean isBusinessException() {
        return true;
    }
}
