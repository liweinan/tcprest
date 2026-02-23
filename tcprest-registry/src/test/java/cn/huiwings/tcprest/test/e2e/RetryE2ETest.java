package cn.huiwings.tcprest.test.e2e;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.discovery.RoundRobinLoadBalancer;
import cn.huiwings.tcprest.registry.InMemoryRegistry;
import cn.huiwings.tcprest.registry.SimpleRetryPolicy;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * E2E: server fails first N times then succeeds; client with RetryPolicy eventually gets success.
 */
public class RetryE2ETest {

    private static final String SERVICE_NAME = "flaky";

    private InMemoryRegistry registry;
    private TcpRestServer server;
    private int port;
    private Flaky client;

    public interface Flaky {
        int getValue();
    }

    public static class FlakyImpl implements Flaky {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final int failFirstCalls;

        public FlakyImpl(int failFirstCalls) {
            this.failFirstCalls = failFirstCalls;
        }

        @Override
        public int getValue() {
            if (callCount.incrementAndGet() <= failFirstCalls) {
                throw new RuntimeException("simulated failure");
            }
            return 42;
        }
    }

    @BeforeClass
    public void setup() throws Exception {
        registry = new InMemoryRegistry();
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        server.addSingletonResource(new FlakyImpl(2)); // fail first 2 calls
        server.setServiceRegistry(registry, SERVICE_NAME, "localhost");
        server.up();
        Thread.sleep(300);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5, 10, false, true);
        TcpRestClientFactory factory = new TcpRestClientFactory(
                registry, SERVICE_NAME, new RoundRobinLoadBalancer(),
                null, retryPolicy, null, null, null, null, Flaky.class);
        client = factory.getClient();
    }

    @AfterClass
    public void teardown() throws Exception {
        if (server != null) server.down();
        Thread.sleep(200);
    }

    @Test
    public void retryUntilSuccess() {
        int value = client.getValue();
        assertEquals(value, 42);
    }

    @Test(dependsOnMethods = "retryUntilSuccess")
    public void subsequentCallSucceedsWithoutRetry() {
        int value = client.getValue();
        assertEquals(value, 42);
    }
}
