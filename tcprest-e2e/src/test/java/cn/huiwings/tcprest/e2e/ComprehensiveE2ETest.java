package cn.huiwings.tcprest.e2e;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.discovery.RoundRobinLoadBalancer;
import cn.huiwings.tcprest.registry.InMemoryRegistry;
import cn.huiwings.tcprest.resilience4j.Resilience4jCircuitBreakerProvider;
import cn.huiwings.tcprest.resilience4j.Resilience4jRetryPolicy;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.ssl.SSLParams;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.testng.Assert.assertEquals;

/**
 * Comprehensive E2E: service discovery (InMemoryRegistry) + Resilience4j (retry + circuit breaker)
 * + Netty server + SSL mutual auth + compression.
 */
public class ComprehensiveE2ETest {

    private static final String SERVICE_NAME = "e2e-echo";

    private InMemoryRegistry registry;
    private TcpRestServer server;
    private int port;
    private EchoService client;

    public interface EchoService {
        String echo(String message);
        int add(int a, int b);
    }

    public static class EchoServiceImpl implements EchoService {
        @Override
        public String echo(String message) {
            return message;
        }
        @Override
        public int add(int a, int b) {
            return a + b;
        }
    }

    @BeforeClass
    public void setup() throws Exception {
        registry = new InMemoryRegistry();
        port = PortGenerator.get();

        SSLParams serverSsl = new SSLParams();
        serverSsl.setTrustStorePath("classpath:server_ks");
        serverSsl.setKeyStorePath("classpath:server_ks");
        serverSsl.setKeyStoreKeyPass("123123");
        serverSsl.setNeedClientAuth(true);

        server = new NettyTcpRestServer(port, serverSsl);
        server.enableCompression();
        server.addSingletonResource(new EchoServiceImpl());
        server.setServiceRegistry(registry, SERVICE_NAME, "localhost");
        server.up();
        Thread.sleep(500);

        SSLParams clientSsl = new SSLParams();
        clientSsl.setTrustStorePath("classpath:client_ks");
        clientSsl.setKeyStorePath("classpath:client_ks");
        clientSsl.setKeyStoreKeyPass("456456");
        clientSsl.setNeedClientAuth(true);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .retryOnException(e -> e instanceof Exception)
                .build();
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(2)
                .slidingWindowSize(10)
                .waitDurationInOpenState(Duration.ofMillis(200))
                .build();

        TcpRestClientFactory factory = new TcpRestClientFactory(
                registry, SERVICE_NAME, new RoundRobinLoadBalancer(),
                new Resilience4jCircuitBreakerProvider(cbConfig),
                new Resilience4jRetryPolicy(retryConfig),
                null, clientSsl, new CompressionConfig(true, 100, 6), null,
                EchoService.class);
        client = factory.getClient();
    }

    @AfterClass
    public void teardown() throws Exception {
        if (server != null) {
            server.down();
        }
        Thread.sleep(300);
    }

    @Test
    public void echoOverSslWithCompression() {
        assertEquals(client.echo("hello"), "hello");
        assertEquals(client.echo("world"), "world");
    }

    @Test
    public void addOverSslWithCompression() {
        assertEquals(client.add(1, 2), 3);
        assertEquals(client.add(10, 20), 30);
    }

    @Test
    public void largePayloadCompressed() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append("data ");
        }
        String payload = sb.toString();
        assertEquals(client.echo(payload), payload);
    }
}
