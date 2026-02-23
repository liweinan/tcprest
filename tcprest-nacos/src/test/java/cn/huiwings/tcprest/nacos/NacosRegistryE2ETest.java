package cn.huiwings.tcprest.nacos;

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
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Properties;

import static org.testng.Assert.assertEquals;

/**
 * E2E test for NacosRegistry: Nacos in Docker (Testcontainers), TcpRest server registers,
 * client discovers and calls.
 */
public class NacosRegistryE2ETest {

    private static final String SERVICE_NAME = "calc-nacos";
    private static final String NACOS_IMAGE = "nacos/nacos-server:v2.3.2";

    private FixedHostPortGenericContainer<?> nacosContainer;
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
            throw new SkipException("Docker not available, skip Nacos E2E");
        }
        nacosContainer = new FixedHostPortGenericContainer<>(NACOS_IMAGE)
                .withFixedExposedPort(8848, 8848)
                .withFixedExposedPort(9848, 9848)
                .withEnv("MODE", "standalone")
                .withEnv("JVM_XMS", "128m")
                .withEnv("JVM_XMX", "256m")
                .waitingFor(Wait.forHttp("/nacos/").forPort(8848).forStatusCodeMatching(code -> code >= 200 && code < 400))
                .withStartupTimeout(java.time.Duration.ofMinutes(3));
        nacosContainer.start();
        String host = nacosContainer.getHost();
        Properties props = new Properties();
        props.setProperty("serverAddr", host + ":8848");
        NacosRegistry nacosRegistry = NacosRegistry.fromProperties(props);
        this.registry = nacosRegistry;
        this.discovery = nacosRegistry;
        Thread.sleep(12_000);

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
        if (nacosContainer != null) {
            nacosContainer.stop();
        }
        Thread.sleep(200);
    }

    @Test
    public void discoveryResolvesAndCallSucceeds() {
        assertEquals(client.add(1, 2), 3);
        assertEquals(client.add(10, 20), 30);
    }
}
