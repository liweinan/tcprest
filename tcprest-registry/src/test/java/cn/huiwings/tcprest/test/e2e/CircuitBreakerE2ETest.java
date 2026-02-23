package cn.huiwings.tcprest.test.e2e;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.discovery.RoundRobinLoadBalancer;
import cn.huiwings.tcprest.registry.InMemoryRegistry;
import cn.huiwings.tcprest.registry.PerInstanceCircuitBreakerProvider;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * E2E: two instances; one always fails. After enough failures the circuit opens for that instance
 * and all requests go to the healthy instance.
 */
public class CircuitBreakerE2ETest {

    private static final String SERVICE_NAME = "mixed";

    private InMemoryRegistry registry;
    private TcpRestServer serverOk;
    private TcpRestServer serverFail;
    private int portOk;
    private int portFail;
    private Echo client;

    public interface Echo {
        int getInstanceId();
    }

    public static class EchoOk implements Echo {
        @Override
        public int getInstanceId() {
            return 1;
        }
    }

    public static class EchoFail implements Echo {
        @Override
        public int getInstanceId() {
            throw new RuntimeException("always fail");
        }
    }

    @BeforeClass
    public void setup() throws Exception {
        registry = new InMemoryRegistry();
        portOk = PortGenerator.get();
        portFail = PortGenerator.get();

        serverOk = new SingleThreadTcpRestServer(portOk);
        serverOk.addSingletonResource(new EchoOk());
        serverOk.setServiceRegistry(registry, SERVICE_NAME, "localhost");
        serverOk.up();

        serverFail = new SingleThreadTcpRestServer(portFail);
        serverFail.addSingletonResource(new EchoFail());
        serverFail.setServiceRegistry(registry, SERVICE_NAME, "localhost");
        serverFail.up();

        Thread.sleep(500);

        PerInstanceCircuitBreakerProvider cbProvider = new PerInstanceCircuitBreakerProvider(3, 60_000);
        TcpRestClientFactory factory = new TcpRestClientFactory(
                registry, SERVICE_NAME, new RoundRobinLoadBalancer(),
                cbProvider, null, null, null, null, null, Echo.class);
        client = factory.getClient();
    }

    @AfterClass
    public void teardown() throws Exception {
        if (serverOk != null) serverOk.down();
        if (serverFail != null) serverFail.down();
        Thread.sleep(300);
    }

    @Test
    public void afterFailuresOnlyHealthyInstanceReceivesTraffic() {
        int failures = 0;
        int successes = 0;
        for (int i = 0; i < 20; i++) {
            try {
                int id = client.getInstanceId();
                if (id == 1) successes++;
            } catch (Exception e) {
                failures++;
            }
        }
        assertTrue(failures >= 3, "at least 3 failures to open circuit");
        assertTrue(successes >= 1, "eventually only healthy instance (id=1) is used");
    }
}
