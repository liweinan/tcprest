package cn.huiwings.tcprest.test.e2e;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.discovery.RoundRobinLoadBalancer;
import cn.huiwings.tcprest.registry.InMemoryRegistry;
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
 * E2E: two instances registered under same service name; client round-robins across both.
 */
public class DiscoveryLoadBalanceE2ETest {

    private static final String SERVICE_NAME = "echo";

    private InMemoryRegistry registry;
    private TcpRestServer server1;
    private TcpRestServer server2;
    private int port1;
    private int port2;
    private Echo client;

    public interface Echo {
        int getInstanceId();
    }

    public static class EchoImpl implements Echo {
        private final int instanceId;

        public EchoImpl(int instanceId) {
            this.instanceId = instanceId;
        }

        @Override
        public int getInstanceId() {
            return instanceId;
        }
    }

    @BeforeClass
    public void setup() throws Exception {
        registry = new InMemoryRegistry();
        port1 = PortGenerator.get();
        port2 = PortGenerator.get();

        server1 = new SingleThreadTcpRestServer(port1);
        server1.addSingletonResource(new EchoImpl(1));
        server1.setServiceRegistry(registry, SERVICE_NAME, "localhost");
        server1.up();

        server2 = new SingleThreadTcpRestServer(port2);
        server2.addSingletonResource(new EchoImpl(2));
        server2.setServiceRegistry(registry, SERVICE_NAME, "localhost");
        server2.up();

        Thread.sleep(500);

        TcpRestClientFactory factory = new TcpRestClientFactory(registry, SERVICE_NAME, new RoundRobinLoadBalancer(), Echo.class);
        client = factory.getClient();
    }

    @AfterClass
    public void teardown() throws Exception {
        if (server1 != null) server1.down();
        if (server2 != null) server2.down();
        Thread.sleep(300);
    }

    @Test
    public void bothInstancesReceiveTraffic() {
        AtomicInteger saw1 = new AtomicInteger(0);
        AtomicInteger saw2 = new AtomicInteger(0);
        for (int i = 0; i < 20; i++) {
            int id = client.getInstanceId();
            if (id == 1) saw1.incrementAndGet();
            else if (id == 2) saw2.incrementAndGet();
        }
        assertTrue(saw1.get() >= 1, "instance 1 should get at least one request");
        assertTrue(saw2.get() >= 1, "instance 2 should get at least one request");
        assertEquals(saw1.get() + saw2.get(), 20);
    }
}
