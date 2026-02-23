package cn.huiwings.tcprest.governance;

import cn.huiwings.tcprest.discovery.HostPort;

/**
 * Provides a {@link CircuitBreaker} for each {@link HostPort}. Used in discovery mode to track
 * per-instance success/failure and filter instances via {@link CircuitBreaker#allowRequest()}.
 *
 * @since 2.0.0
 */
public interface CircuitBreakerProvider {

    /**
     * Return the circuit breaker for the given instance. Must return a non-null instance.
     *
     * @param hostPort instance address
     * @return circuit breaker for that instance
     */
    CircuitBreaker get(HostPort hostPort);
}
