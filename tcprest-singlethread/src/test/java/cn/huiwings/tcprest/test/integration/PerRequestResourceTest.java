package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Test addResource (per-request instances) behavior.
 *
 * <p>Verifies that each request gets a new instance when using addResource,
 * so state is NOT shared between requests.</p>
 */
public class PerRequestResourceTest {

    // Use dedicated port range for this test class (24000-24999)
    private static final PortGenerator.PortRange portRange = PortGenerator.from(24000);

    private TcpRestServer server;
    private int port;

    @BeforeClass
    public void setup() throws Exception {
        port = portRange.next();
        server = new SingleThreadTcpRestServer(port);

        // Test addResource - new instance per request
        server.addResource(CounterServiceImpl.class);

        server.up();
        Thread.sleep(500);
    }

    @AfterClass
    public void tearDown() throws Exception {
        if (server != null) {
            server.down();
        }
        Thread.sleep(500);
    }

    // ========== Test Per-Request Resource ==========

    @Test
    public void testPerRequestResource_noSharedState() {
        // Create v2 client
        TcpRestClientFactory factory = new TcpRestClientFactory(
            CounterService.class, "localhost", port
        );
        CounterService client = (CounterService) factory.getClient();

        // Each call gets a new instance, so count always starts at 0
        int count1 = client.increment();
        assertEquals(count1, 1, "First call should increment from 0 to 1");

        int count2 = client.increment();
        assertEquals(count2, 1, "Second call should also increment from 0 to 1 (new instance)");

        int count3 = client.increment();
        assertEquals(count3, 1, "Third call should also increment from 0 to 1 (new instance)");

        // Verify no state is shared (all return same value)
        assertEquals(count1, count2);
        assertEquals(count2, count3);
    }

    @Test
    public void testPerRequestResource_getCountAlwaysZero() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
            CounterService.class, "localhost", port
        );
        CounterService client = (CounterService) factory.getClient();

        // Each call gets a new instance, so count is always 0
        int count1 = client.getCount();
        assertEquals(count1, 0);

        int count2 = client.getCount();
        assertEquals(count2, 0);

        // Even after increment, next getCount() gets new instance
        client.increment();
        int count3 = client.getCount();
        assertEquals(count3, 0, "New instance should have count=0");
    }

    @Test
    public void testPerRequestResource_overloadedMethods() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
            CounterService.class, "localhost", port
        );
        CounterService client = (CounterService) factory.getClient();

        // Test overloaded increment methods (each gets new instance)
        int result1 = client.increment();      // new instance, 0 + 1 = 1
        assertEquals(result1, 1);

        int result2 = client.increment(5);     // new instance, 0 + 5 = 5
        assertEquals(result2, 5);

        int result3 = client.increment(10);    // new instance, 0 + 10 = 10
        assertEquals(result3, 10);

        // Verify no state accumulation
        assertNotEquals(result3, result1 + result2 + result3);
    }

    @Test
    public void testPerRequestResource_reset() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
            CounterService.class, "localhost", port
        );
        CounterService client = (CounterService) factory.getClient();

        // Reset on a new instance (no-op but should not fail)
        client.reset();

        // Count should still be 0 (new instance)
        int count = client.getCount();
        assertEquals(count, 0);
    }

    // ========== Test Service Interface ==========

    /**
     * Counter service with state (for testing per-request behavior).
     */
    public interface CounterService {
        int increment();
        int increment(int delta);
        int getCount();
        void reset();
    }

    /**
     * Stateful implementation (each request gets a new instance).
     */
    public static class CounterServiceImpl implements CounterService {
        private int count = 0;

        @Override
        public int increment() {
            return ++count;
        }

        @Override
        public int increment(int delta) {
            count += delta;
            return count;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public void reset() {
            count = 0;
        }
    }
}
