package cn.huiwings.tcprest.registry;

import cn.huiwings.tcprest.discovery.HostPort;
import cn.huiwings.tcprest.governance.CircuitBreaker;
import cn.huiwings.tcprest.governance.CircuitBreakerProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides one {@link CircuitBreakerImpl} per {@link HostPort}. Use with discovery-based client
 * to track per-instance failures and skip open instances.
 *
 * @since 2.0.0
 */
public class PerInstanceCircuitBreakerProvider implements CircuitBreakerProvider {

    private final int failureThreshold;
    private final long openTimeoutMs;
    private final Map<HostPort, CircuitBreaker> map = new ConcurrentHashMap<>();

    public PerInstanceCircuitBreakerProvider(int failureThreshold, long openTimeoutMs) {
        this.failureThreshold = failureThreshold;
        this.openTimeoutMs = openTimeoutMs;
    }

    @Override
    public CircuitBreaker get(HostPort hostPort) {
        return map.computeIfAbsent(hostPort, hp -> new CircuitBreakerImpl(failureThreshold, openTimeoutMs));
    }
}
