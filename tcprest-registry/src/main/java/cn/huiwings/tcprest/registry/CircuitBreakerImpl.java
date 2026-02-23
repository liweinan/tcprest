package cn.huiwings.tcprest.registry;

import cn.huiwings.tcprest.governance.CircuitBreaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple circuit breaker: CLOSED → (failures >= threshold) → OPEN → (after timeout) → HALF_OPEN → (success) → CLOSED.
 * Thread-safe.
 *
 * @since 2.0.0
 */
public class CircuitBreakerImpl implements CircuitBreaker {

    private static final int STATE_CLOSED = 0;
    private static final int STATE_OPEN = 1;
    private static final int STATE_HALF_OPEN = 2;

    private final int failureThreshold;
    private final long openTimeoutMs;
    private final AtomicInteger state = new AtomicInteger(STATE_CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong openSince = new AtomicLong(0);

    /**
     * @param failureThreshold number of consecutive failures before opening
     * @param openTimeoutMs    time in OPEN state before allowing one request (HALF_OPEN)
     */
    public CircuitBreakerImpl(int failureThreshold, long openTimeoutMs) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be >= 1");
        }
        if (openTimeoutMs < 0) {
            throw new IllegalArgumentException("openTimeoutMs must be >= 0");
        }
        this.failureThreshold = failureThreshold;
        this.openTimeoutMs = openTimeoutMs;
    }

    @Override
    public boolean allowRequest() {
        for (; ; ) {
            int s = state.get();
            if (s == STATE_CLOSED) {
                return true;
            }
            if (s == STATE_OPEN) {
                if (openTimeoutMs == 0) {
                    return false;
                }
                long open = openSince.get();
                if (System.currentTimeMillis() - open >= openTimeoutMs) {
                    if (state.compareAndSet(STATE_OPEN, STATE_HALF_OPEN)) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
            if (s == STATE_HALF_OPEN) {
                return true;
            }
        }
    }

    @Override
    public void recordSuccess() {
        if (state.compareAndSet(STATE_HALF_OPEN, STATE_CLOSED)) {
            failureCount.set(0);
        } else if (state.get() == STATE_CLOSED) {
            failureCount.set(0);
        }
    }

    @Override
    public void recordFailure() {
        for (; ; ) {
            int s = state.get();
            if (s == STATE_CLOSED) {
                int n = failureCount.incrementAndGet();
                if (n >= failureThreshold) {
                    if (state.compareAndSet(STATE_CLOSED, STATE_OPEN)) {
                        openSince.set(System.currentTimeMillis());
                    }
                }
                return;
            }
            if (s == STATE_HALF_OPEN) {
                state.set(STATE_OPEN);
                openSince.set(System.currentTimeMillis());
                return;
            }
            if (s == STATE_OPEN) {
                return;
            }
        }
    }

    @Override
    public void reset() {
        state.set(STATE_CLOSED);
        failureCount.set(0);
        openSince.set(0);
    }
}
