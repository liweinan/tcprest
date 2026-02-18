package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.ssl.SSLParams;
import cn.huiwings.tcprest.test.HelloWorld;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * SSL smoke test for NettyTcpRestServer.
 *
 * <p>Tests SSL/TLS functionality with both one-way and two-way handshakes.</p>
 *
 * @author Weinan Li
 * @date 2026-02-18
 */
public class NettySSLSmokeTest {
    protected TcpRestServer tcpRestServer;

    @BeforeMethod
    public void startTcpRestServer() throws Exception {
        SSLParams serverSSLParams = new SSLParams();
        serverSSLParams.setTrustStorePath("classpath:server_ks");
        serverSSLParams.setKeyStorePath("classpath:server_ks");
        serverSSLParams.setKeyStoreKeyPass("123123");
        serverSSLParams.setNeedClientAuth(true);

        tcpRestServer = new NettyTcpRestServer(PortGenerator.get(), serverSSLParams);
        tcpRestServer.up();
        // Allow async server to fully start
        Thread.sleep(500);
    }

    @AfterMethod
    public void stopTcpRestServer() throws Exception {
        tcpRestServer.down();
        // Wait for port to be fully released
        Thread.sleep(300);
    }

    @Test
    public void testTwoWayHandShake() {
        tcpRestServer.addResource(HelloWorldResource.class);

        SSLParams clientSSLParams = new SSLParams();
        clientSSLParams.setTrustStorePath("classpath:client_ks");
        clientSSLParams.setKeyStorePath("classpath:client_ks");
        clientSSLParams.setKeyStoreKeyPass("456456");
        clientSSLParams.setNeedClientAuth(true);

        TcpRestClientFactory factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost",
                        tcpRestServer.getServerPort(), null, clientSSLParams);

        HelloWorld client = factory.getInstance();

        Assert.assertEquals("Hello, World", client.sayHelloTo("World"));
    }

    @Test
    public void testOneWayHandShake() {
        tcpRestServer.addResource(HelloWorldResource.class);

        // Client without SSL authentication (one-way SSL)
        SSLParams clientSSLParams = new SSLParams();
        clientSSLParams.setTrustStorePath("classpath:client_ks");
        clientSSLParams.setNeedClientAuth(false);

        TcpRestClientFactory factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost",
                        tcpRestServer.getServerPort(), null, clientSSLParams);

        HelloWorld client = factory.getInstance();

        Assert.assertEquals("Hello, world!", client.helloWorld());
    }

    @Test
    public void testSSLWithLargePayload() {
        tcpRestServer.addResource(HelloWorldResource.class);

        SSLParams clientSSLParams = new SSLParams();
        clientSSLParams.setTrustStorePath("classpath:client_ks");
        clientSSLParams.setKeyStorePath("classpath:client_ks");
        clientSSLParams.setKeyStoreKeyPass("456456");
        clientSSLParams.setNeedClientAuth(true);

        TcpRestClientFactory factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost",
                        tcpRestServer.getServerPort(), null, clientSSLParams);

        HelloWorld client = factory.getInstance();

        // Test large data over SSL (10KB)
        StringBuilder builder = new StringBuilder();
        String[] alpha = {"a", "b", "c", "d", "e", "f"};
        for (int i = 0; i < 1024 * 10; i++) {
            builder.append(alpha[i % alpha.length]);
        }
        String req = builder.toString();

        String response = client.echo(req);
        Assert.assertEquals(req, response);
    }
}
