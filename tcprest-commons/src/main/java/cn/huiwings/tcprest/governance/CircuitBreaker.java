package cn.huiwings.tcprest.governance;

/**
 * Circuit breaker for a single instance (e.g. a {@link cn.huiwings.tcprest.discovery.HostPort}).
 * Call {@link #allowRequest()} before sending a request; call {@link #recordSuccess()} or
 * {@link #recordFailure()} after the request completes.
 *
 * @since 2.0.0
 */
public interface CircuitBreaker {

    /**
     * Whether a request is allowed (circuit not open).
     *
     * @return true if the caller may send a request
     */
    boolean allowRequest();

    /**
     * Record a successful request.
     */
    void recordSuccess();

    /**
     * Record a failed request.
     */
    void recordFailure();

    /**
     * Reset state (optional). Used for testing or manual recovery.
     */
    void reset();
}
