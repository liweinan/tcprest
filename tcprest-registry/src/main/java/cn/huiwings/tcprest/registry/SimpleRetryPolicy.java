package cn.huiwings.tcprest.registry;

import cn.huiwings.tcprest.exception.NoInstanceException;
import cn.huiwings.tcprest.exception.TimeoutException;
import cn.huiwings.tcprest.governance.RetryPolicy;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;

/**
 * Simple retry policy: configurable max attempts, delay, and retryable exceptions.
 * By default retries on connection/timeout/network errors, not on business or protocol errors.
 *
 * @since 2.0.0
 */
public class SimpleRetryPolicy implements RetryPolicy {

    private final int maxAttempts;
    private final long delayMs;
    private final boolean retryOnNoInstance;
    private final boolean retryOnAny;

    /**
     * @param maxAttempts max attempts (including first call); must be >= 1
     * @param delayMs     delay between attempts in ms (0 = no delay)
     */
    public SimpleRetryPolicy(int maxAttempts, long delayMs) {
        this(maxAttempts, delayMs, false, false);
    }

    /**
     * @param maxAttempts        max attempts (including first call); must be >= 1
     * @param delayMs            delay between attempts in ms (0 = no delay)
     * @param retryOnNoInstance  if true, treat {@link NoInstanceException} as retryable (e.g. for discovery race)
     */
    public SimpleRetryPolicy(int maxAttempts, long delayMs, boolean retryOnNoInstance) {
        this(maxAttempts, delayMs, retryOnNoInstance, false);
    }

    /**
     * @param maxAttempts        max attempts (including first call); must be >= 1
     * @param delayMs            delay between attempts in ms (0 = no delay)
     * @param retryOnNoInstance  if true, treat {@link NoInstanceException} as retryable
     * @param retryOnAny         if true, treat any Throwable as retryable (e.g. for tests or generic retry)
     */
    public SimpleRetryPolicy(int maxAttempts, long delayMs, boolean retryOnNoInstance, boolean retryOnAny) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
        this.delayMs = delayMs;
        this.retryOnNoInstance = retryOnNoInstance;
        this.retryOnAny = retryOnAny;
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }

    @Override
    public boolean isRetryable(Throwable throwable) {
        if (retryOnAny) {
            return true;
        }
        if (retryOnNoInstance && throwable instanceof NoInstanceException) {
            return true;
        }
        if (throwable instanceof TimeoutException) {
            return true;
        }
        if (throwable instanceof ConnectException || throwable instanceof SocketException) {
            return true;
        }
        if (throwable instanceof IOException) {
            return true;
        }
        Throwable cause = throwable.getCause();
        return cause != null && cause != throwable && isRetryable(cause);
    }

    @Override
    public long getDelayMs(int attempt) {
        return delayMs;
    }
}
