package cn.huiwings.tcprest.governance;

/**
 * Policy for retrying failed requests. Used by the client proxy to decide whether and how to retry.
 *
 * @since 2.0.0
 */
public interface RetryPolicy {

    /**
     * Maximum number of attempts (including the first call). Must be at least 1.
     *
     * @return max attempts
     */
    int getMaxAttempts();

    /**
     * Whether the given throwable is retryable.
     *
     * @param throwable the failure
     * @return true if the client should retry
     */
    boolean isRetryable(Throwable throwable);

    /**
     * Delay in milliseconds before the next attempt (0 = no delay). Called with attempt index 1-based
     * (first retry = 1, second retry = 2, ...).
     *
     * @param attempt 1-based attempt number (1 = first retry)
     * @return delay in ms before next attempt
     */
    long getDelayMs(int attempt);
}
