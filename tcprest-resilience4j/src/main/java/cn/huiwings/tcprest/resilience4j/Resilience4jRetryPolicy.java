package cn.huiwings.tcprest.resilience4j;

import cn.huiwings.tcprest.governance.RetryPolicy;
import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Either;

import java.util.function.Predicate;

/**
 * Adapts Resilience4j {@link RetryConfig} to TcpRest {@link RetryPolicy}.
 * Uses the config's max attempts, retry predicate, and {@link IntervalBiFunction} for delay.
 * Delay is taken from {@link RetryConfig#getIntervalBiFunction()}; when null, {@value #DEFAULT_DELAY_MS} ms is used.
 * For custom delay-by-attempt, configure {@code intervalBiFunction} on the config (e.g. {@code IntervalBiFunction.ofIntervalFunction(...)}).
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
        IntervalBiFunction<Object> biFn = config.getIntervalBiFunction();
        if (biFn != null) {
            return biFn.apply(attempt, Either.left(new Throwable("retry")));
        }
        return DEFAULT_DELAY_MS;
    }
}
