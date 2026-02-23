package cn.huiwings.tcprest.resilience4j;

import cn.huiwings.tcprest.governance.CircuitBreaker;

import java.util.concurrent.TimeUnit;

/**
 * Adapts Resilience4j's circuit breaker to TcpRest {@link CircuitBreaker}.
 * Delegates allowRequest → tryAcquirePermission, recordSuccess → onSuccess, recordFailure → onError, reset → reset.
 *
 * @since 2.0.0
 */
public class Resilience4jCircuitBreakerAdapter implements CircuitBreaker {

    private final io.github.resilience4j.circuitbreaker.CircuitBreaker delegate;

    public Resilience4jCircuitBreakerAdapter(io.github.resilience4j.circuitbreaker.CircuitBreaker delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean allowRequest() {
        return delegate.tryAcquirePermission();
    }

    @Override
    public void recordSuccess() {
        delegate.onSuccess(0, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordFailure() {
        delegate.onError(0, TimeUnit.NANOSECONDS, null);
    }

    @Override
    public void reset() {
        delegate.reset();
        delegate.transitionToClosedState();
    }
}
