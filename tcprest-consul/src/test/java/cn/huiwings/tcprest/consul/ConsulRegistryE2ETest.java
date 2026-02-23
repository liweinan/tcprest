package cn.huiwings.tcprest.consul;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.discovery.RoundRobinLoadBalancer;
import cn.huiwings.tcprest.discovery.ServiceDiscovery;
import cn.huiwings.tcprest.discovery.ServiceRegistry;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.testng.Assert.assertEquals;

/**
 * E2E test for ConsulRegistry: Consul in Docker (Testcontainers), TcpRest server registers,
 * client discovers and calls.
 */
public class ConsulRegistryE2ETest {

    private static final String SERVICE_NAME = "calc-consul";
    private static final String CONSUL_IMAGE = "hashicorp/consul:1.17";

    private GenericContainer<?> consulContainer;
    private ServiceRegistry registry;
    private ServiceDiscovery discovery;
    private TcpRestServer server;
    private int serverPort;
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
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new SkipException("Docker not available, skip Consul E2E");
        }
        consulContainer = new GenericContainer<>(CONSUL_IMAGE)
                .withCommand("agent", "-dev", "-client", "0.0.0.0", "-ui")
                .withExposedPorts(8500)
                .waitingFor(Wait.forHttp("/v1/status/leader").forPort(8500).forStatusCode(200))
                .withStartupTimeout(java.time.Duration.ofMinutes(2));
        consulContainer.start();

        String host = consulContainer.getHost();
        int port = consulContainer.getMappedPort(8500);
        ConsulRegistry consulRegistry = new ConsulRegistry(host, port);
        this.registry = consulRegistry;
        this.discovery = consulRegistry;

        serverPort = PortGenerator.get();
        server = new SingleThreadTcpRestServer(serverPort);
        server.addSingletonResource(new CalculatorImpl());
        server.setServiceRegistry(registry, SERVICE_NAME, "localhost");
        server.up();
        Thread.sleep(400);

        TcpRestClientFactory factory = new TcpRestClientFactory(
                discovery, SERVICE_NAME, new RoundRobinLoadBalancer(), Calculator.class);
        client = factory.getClient();
    }

    @AfterClass
    public void teardown() throws Exception {
        if (server != null) {
            server.down();
        }
        if (consulContainer != null) {
            consulContainer.stop();
        }
        Thread.sleep(200);
    }

    @Test
    public void discoveryResolvesAndCallSucceeds() {
        assertEquals(client.add(1, 2), 3);
        assertEquals(client.add(10, 20), 30);
    }
}
