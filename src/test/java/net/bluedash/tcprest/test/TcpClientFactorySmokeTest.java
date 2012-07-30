package net.bluedash.tcprest.test;

import net.bluedash.tcprest.client.TcpRestClientFactory;
import net.bluedash.tcprest.client.TcpRestClientProxy;
import net.bluedash.tcprest.server.SingleThreadTcpRestServer;
import net.bluedash.tcprest.server.TcpRestServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Random;

import static junit.framework.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class TcpClientFactorySmokeTest {
    protected TcpRestServer tcpRestServer;


    @Before
    public void startTcpRestServer() throws IOException {

        tcpRestServer = new SingleThreadTcpRestServer(Math.abs(new Random().nextInt()) % 10000 + 8000);
        tcpRestServer.up();
    }

    @After
    public void stopTcpRestServer() throws IOException {
        tcpRestServer.down();
    }


    @Test
    public void testClient() throws IOException {

        tcpRestServer.addResource(HelloWorldResource.class);

        TcpRestClientFactory factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost",
                ((SingleThreadTcpRestServer) tcpRestServer).getServerSocket().getLocalPort());
        HelloWorld client = (HelloWorld) factory.getInstance();
        assertEquals("Hello, world!", client.helloWorld());


    }

    @Test
    public void testProxy() throws IOException {

        tcpRestServer.addResource(HelloWorldResource.class);

        HelloWorld client = (HelloWorld) Proxy.newProxyInstance(HelloWorld.class.getClassLoader(),
                new Class[]{HelloWorld.class}, new TcpRestClientProxy(HelloWorld.class.getCanonicalName(), "localhost",
                    ((SingleThreadTcpRestServer) tcpRestServer).getServerSocket().getLocalPort()));

        assertEquals("Hello, world!", client.helloWorld());
        assertEquals("x,2,false", client.oneTwoThree("x", 2, false));
    }


}
