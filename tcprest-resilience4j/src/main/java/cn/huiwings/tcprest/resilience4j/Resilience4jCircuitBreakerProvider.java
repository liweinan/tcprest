package cn.huiwings.tcprest.resilience4j;

import cn.huiwings.tcprest.discovery.HostPort;
import cn.huiwings.tcprest.governance.CircuitBreaker;
import cn.huiwings.tcprest.governance.CircuitBreakerProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Provides a Resilience4j-backed {@link CircuitBreaker} per {@link HostPort}.
 * Uses a {@link CircuitBreakerRegistry} keyed by "host:port".
 *
 * @since 2.0.0
 */
public class Resilience4jCircuitBreakerProvider implements CircuitBreakerProvider {

    private final CircuitBreakerRegistry registry;

    /**
     * Create with default registry (default CircuitBreakerConfig).
     */
    public Resilience4jCircuitBreakerProvider() {
        this(CircuitBreakerRegistry.ofDefaults());
    }

    /**
     * Create with custom config (same config for all instances).
     */
    public Resilience4jCircuitBreakerProvider(CircuitBreakerConfig config) {
        this(CircuitBreakerRegistry.of(config));
    }

    /**
     * Create with existing registry.
     */
    public Resilience4jCircuitBreakerProvider(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public CircuitBreaker get(HostPort hostPort) {
        String name = hostPort.getHost() + ":" + hostPort.getPort();
        return new Resilience4jCircuitBreakerAdapter(registry.circuitBreaker(name));
    }
}
