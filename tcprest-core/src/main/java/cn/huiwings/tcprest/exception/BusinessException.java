package cn.huiwings.tcprest.exception;

/**
 * Marker exception for business logic errors.
 *
 * <p>Business exceptions represent expected error conditions in application
 * logic, such as:</p>
 * <ul>
 *   <li>Validation failures (invalid input)</li>
 *   <li>Business rule violations</li>
 *   <li>Authorization failures</li>
 *   <li>Resource not found</li>
 * </ul>
 *
 * <p>Unlike server errors (NullPointerException, etc.), business exceptions
 * are expected and handled as part of normal application flow.</p>
 *
 * <p>BusinessException maps to StatusCode.BUSINESS_EXCEPTION (1) in Protocol v2.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * public class ValidationException extends BusinessException {
 *     public ValidationException(String message) {
 *         super(message);
 *     }
 * }
 *
 * // In service method:
 * if (age < 0) {
 *     throw new ValidationException("Age must be non-negative");
 * }
 * </pre>
 *
 * @since 1.1.0
 */
public class BusinessException extends RuntimeException {

    /**
     * Constructs a new business exception with the specified detail message.
     *
     * @param message the detail message
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Constructs a new business exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new business exception with the specified cause.
     *
     * @param cause the cause
     */
    public BusinessException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new business exception with no detail message.
     */
    public BusinessException() {
        super();
    }
}
