package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.ssl.SSLParams;
import cn.huiwings.tcprest.test.HelloWorld;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Random;

/**
 * @author Weinan Li
 * @created_at 08 25 2012
 */
public class SSLSmokeTest {
    protected TcpRestServer tcpRestServer;

    @BeforeMethod
    public void startTcpRestServer() throws Exception {
        SSLParams serverSSLParams = new SSLParams();
        serverSSLParams.setTrustStorePath("classpath:server_ks");
        serverSSLParams.setKeyStorePath("classpath:server_ks");
        serverSSLParams.setKeyStoreKeyPass("123123");
        serverSSLParams.setNeedClientAuth(true);

        tcpRestServer = new SingleThreadTcpRestServer(Math.abs(new Random().nextInt()) % 10000 + 8000, serverSSLParams);
        tcpRestServer.up();
    }

    @AfterMethod
    public void stopTcpRestServer() throws Exception {
        tcpRestServer.down();
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
                new TcpRestClientFactory(HelloWorld.class, "localhost", tcpRestServer.getServerPort(), null, clientSSLParams);

        HelloWorld client = (HelloWorld) factory.getInstance();

        Assert.assertEquals("Hello, World", client.sayHelloTo("World"));

    }
}
