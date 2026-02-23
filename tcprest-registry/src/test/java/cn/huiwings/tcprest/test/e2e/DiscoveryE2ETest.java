package cn.huiwings.tcprest.test.e2e;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.discovery.RoundRobinLoadBalancer;
import cn.huiwings.tcprest.exception.NoInstanceException;
import cn.huiwings.tcprest.registry.InMemoryRegistry;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

/**
 * E2E: client uses service discovery to resolve and call a registered server.
 */
public class DiscoveryE2ETest {

    private static final String SERVICE_NAME = "calc";

    private InMemoryRegistry registry;
    private TcpRestServer server;
    private int port;
    private Calculator client;

    public interface Calculator {
        int add(int a, int b);
    }

    public static class CalculatorImpl implements Calculator {
        @Override
        public int add(int a, int b) {
            return a + b;
        }
    }

    @BeforeClass
    public void setup() throws Exception {
        registry = new InMemoryRegistry();
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        server.addSingletonResource(new CalculatorImpl());
        server.setServiceRegistry(registry, SERVICE_NAME, "localhost");
        server.up();
        Thread.sleep(300);

        TcpRestClientFactory factory = new TcpRestClientFactory(registry, SERVICE_NAME, new RoundRobinLoadBalancer(), Calculator.class);
        client = factory.getClient();
    }

    @AfterClass
    public void teardown() throws Exception {
        if (server != null) {
            server.down();
        }
        Thread.sleep(200);
    }

    @Test
    public void discoveryResolvesAndCallSucceeds() {
        assertEquals(client.add(1, 2), 3);
        assertEquals(client.add(10, 20), 30);
    }

    @Test(dependsOnMethods = "discoveryResolvesAndCallSucceeds")
    public void noInstanceAfterDeregister() throws Exception {
        server.down();
        Thread.sleep(200);
        registry.deregister(SERVICE_NAME, "localhost", port);

        expectThrows(NoInstanceException.class, () -> client.add(1, 2));
    }
}
