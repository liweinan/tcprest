package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.ssl.SSLParam;
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
        SSLParam serverSSLParam = new SSLParam();
        serverSSLParam.setTrustStorePath("classpath:server_ks");
        serverSSLParam.setKeyStorePath("classpath:server_ks");
        serverSSLParam.setKeyStoreKeyPass("123123");
        serverSSLParam.setNeedClientAuth(true);

        tcpRestServer = new SingleThreadTcpRestServer(Math.abs(new Random().nextInt()) % 10000 + 8000, serverSSLParam);
        tcpRestServer.up();
    }

    @AfterMethod
    public void stopTcpRestServer() throws Exception {
        tcpRestServer.down();
    }

    @Test
    public void testTwoWayHandShake() {

        tcpRestServer.addResource(HelloWorldResource.class);

        SSLParam clientSSLParam = new SSLParam();
        clientSSLParam.setTrustStorePath("classpath:client_ks");
        clientSSLParam.setKeyStorePath("classpath:client_ks");
        clientSSLParam.setKeyStoreKeyPass("456456");
        clientSSLParam.setNeedClientAuth(true);

        TcpRestClientFactory factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost", tcpRestServer.getServerPort(), null, clientSSLParam);

        HelloWorld client = (HelloWorld) factory.getInstance();

        Assert.assertEquals("Hello, World", client.sayHelloTo("World"));

    }
}
