package cn.huiwings.tcprest.test;


import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Random;

@Test
public class HelloWorldTest {

    protected TcpRestServer tcpRestServer;
    static int port = Math.abs(new Random().nextInt()) % 10000 + 8000;

    @BeforeMethod
    public void startTcpRestServer() throws Exception {

        tcpRestServer = new NettyTcpRestServer(port);
        tcpRestServer.up();
        tcpRestServer.addResource(HelloWorldResource.class);
        // Server port: port
    }

    @AfterMethod
    public void stopTcpRestServer() throws IOException {
        tcpRestServer.down();
    }


    @Test
    public void testHello() {
        tcpRestServer.addResource(HelloWorldResource.class);
        TcpRestClientFactory factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost", port);
        HelloWorld client = factory.getInstance();

        String resp = client.echo("Hello, world!");
        // Response: resp
    }
}
