package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.NettyUdpRestClientFactory;
import cn.huiwings.tcprest.server.NettyUdpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.HelloWorld;
import cn.huiwings.tcprest.test.HelloWorldResource;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * E2E test: NettyUdpRestServer + NettyUdpRestClientFactory over UDP.
 */
public class NettyUdpE2ETest {

    private TcpRestServer server;
    private NettyUdpRestClientFactory clientFactory;
    private int port;

    @BeforeClass
    public void setUp() throws Exception {
        port = PortGenerator.get();
        server = new NettyUdpRestServer(port);
        server.addResource(HelloWorldResource.class);
        server.up();
        Thread.sleep(300);
        clientFactory = new NettyUdpRestClientFactory(HelloWorld.class, "localhost", port);
    }

    @AfterClass
    public void tearDown() throws Exception {
        if (clientFactory != null) {
            clientFactory.shutdown();
        }
        if (server != null) {
            server.down();
        }
        Thread.sleep(200);
    }

    @Test
    public void udpRequestResponse_helloWorld() {
        HelloWorld client = clientFactory.getClient();
        assertEquals(client.helloWorld(), "Hello, world!");
    }

    @Test
    public void udpRequestResponse_sayHelloTo() {
        HelloWorld client = clientFactory.getClient();
        assertEquals(client.sayHelloTo("UDP"), "Hello, UDP");
    }
}
