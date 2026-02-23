package cn.huiwings.tcprest.resilience4j;

import cn.huiwings.tcprest.governance.RetryPolicy;
import io.github.resilience4j.retry.RetryConfig;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Adapts Resilience4j {@link RetryConfig} to TcpRest {@link RetryPolicy}.
 * Uses the config's max attempts, retry predicate, and interval function (or default delay) for delays.
 *
 * @since 2.0.0
 */
public class Resilience4jRetryPolicy implements RetryPolicy {

    private static final long DEFAULT_DELAY_MS = 500L;

    private final RetryConfig config;

    public Resilience4jRetryPolicy(RetryConfig config) {
        this.config = config;
    }

    @Override
    public int getMaxAttempts() {
        return config.getMaxAttempts();
    }

    @Override
    public boolean isRetryable(Throwable throwable) {
        Predicate<Throwable> predicate = config.getExceptionPredicate();
        return predicate != null && predicate.test(throwable);
    }

    @Override
    public long getDelayMs(int attempt) {
        Function<Integer, Long> intervalFn = config.getIntervalFunction();
        if (intervalFn != null) {
            return intervalFn.apply(attempt);
        }
        return DEFAULT_DELAY_MS;
    }
}
