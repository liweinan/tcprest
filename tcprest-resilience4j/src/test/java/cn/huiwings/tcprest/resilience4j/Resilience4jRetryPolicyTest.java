package cn.huiwings.tcprest.resilience4j;

import cn.huiwings.tcprest.governance.RetryPolicy;
import io.github.resilience4j.retry.RetryConfig;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for {@link Resilience4jRetryPolicy}.
 */
public class Resilience4jRetryPolicyTest {

    @Test
    public void getMaxAttemptsAndDelayFromConfig() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(5)
                .retryOnException(e -> e instanceof IOException)
                .build();
        RetryPolicy policy = new Resilience4jRetryPolicy(config);
        assertEquals(policy.getMaxAttempts(), 5);
        assertTrue(policy.isRetryable(new IOException()));
        assertFalse(policy.isRetryable(new RuntimeException()));
        assertTrue(policy.getDelayMs(1) >= 0);
    }
}
