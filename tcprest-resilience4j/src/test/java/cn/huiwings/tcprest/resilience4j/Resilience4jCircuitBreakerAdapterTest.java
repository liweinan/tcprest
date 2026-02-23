package cn.huiwings.tcprest.resilience4j;

import cn.huiwings.tcprest.discovery.HostPort;
import cn.huiwings.tcprest.governance.CircuitBreaker;
import cn.huiwings.tcprest.governance.CircuitBreakerProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for {@link Resilience4jCircuitBreakerAdapter} and {@link Resilience4jCircuitBreakerProvider}.
 */
public class Resilience4jCircuitBreakerAdapterTest {

    @Test
    public void allowRequestAndRecordSuccessFailure() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(2)
                .slidingWindowSize(4)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreakerProvider provider = new Resilience4jCircuitBreakerProvider(registry);
        HostPort hp = new HostPort("localhost", 8080);
        CircuitBreaker cb = provider.get(hp);

        assertTrue(cb.allowRequest());
        cb.recordSuccess();
        cb.recordFailure();
        cb.recordFailure();
        assertFalse(cb.allowRequest());
        cb.reset();
        assertTrue(cb.allowRequest(), "after reset circuit should allow requests again");
    }
}
