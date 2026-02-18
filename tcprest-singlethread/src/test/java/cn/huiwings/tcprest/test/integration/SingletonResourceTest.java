package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.protocol.ProtocolVersion;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Test both addResource (per-request instances) and addSingletonResource (shared instance).
 *
 * <p>Verifies that Protocol v2 works correctly with both resource registration methods.</p>
 */
public class SingletonResourceTest {

    // Use dedicated port range for this test class (23000-23999)
    private static final PortGenerator.PortRange portRange = PortGenerator.from(23000);

    private TcpRestServer server;
    private int port;

    @BeforeClass
    public void setup() throws Exception {
        port = portRange.next();
        server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.V2);

        // Test addSingletonResource - single shared instance
        server.addSingletonResource(new CounterServiceImpl());

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

    // ========== Test Singleton Resource ==========

    @Test
    public void testSingletonResource_sharedState() {
        // Create v2 client
        TcpRestClientFactory factory = new TcpRestClientFactory(
            CounterService.class, "localhost", port
        );
        factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        CounterService client = (CounterService) factory.getClient();

        // First call increments to 1
        int count1 = client.increment();
        assertEquals(count1, 1);

        // Second call increments to 2 (state is shared)
        int count2 = client.increment();
        assertEquals(count2, 2);

        // Third call increments to 3
        int count3 = client.increment();
        assertEquals(count3, 3);

        // Verify state is shared across calls
        assertTrue(count3 > count1);
    }

    @Test
    public void testSingletonResource_getState() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
            CounterService.class, "localhost", port
        );
        factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        CounterService client = (CounterService) factory.getClient();

        // Get current count (should be non-zero if other tests ran)
        int currentCount = client.getCount();
        assertTrue(currentCount >= 0);

        // Increment
        client.increment();

        // Verify count increased
        int newCount = client.getCount();
        assertEquals(newCount, currentCount + 1);
    }

    @Test
    public void testSingletonResource_reset() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
            CounterService.class, "localhost", port
        );
        factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        CounterService client = (CounterService) factory.getClient();

        // Increment a few times
        client.increment();
        client.increment();

        // Reset
        client.reset();

        // Verify reset
        int count = client.getCount();
        assertEquals(count, 0);
    }

    @Test
    public void testSingletonResource_overloadedMethods() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
            CounterService.class, "localhost", port
        );
        factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        CounterService client = (CounterService) factory.getClient();

        // Reset to known state
        client.reset();

        // Test overloaded increment methods
        int result1 = client.increment();      // increment by 1
        assertEquals(result1, 1);

        int result2 = client.increment(5);     // increment by 5
        assertEquals(result2, 6);

        int result3 = client.increment(10);    // increment by 10
        assertEquals(result3, 16);
    }

    // ========== Test Service Interface ==========

    /**
     * Counter service with state (for testing singleton behavior).
     */
    public interface CounterService {
        int increment();
        int increment(int delta);
        int getCount();
        void reset();
    }

    /**
     * Stateful implementation (demonstrates singleton behavior).
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
