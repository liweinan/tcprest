package io.tcprest.test;

import io.tcprest.client.TcpRestClientFactory;
import io.tcprest.client.TcpRestClientProxy;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.server.TcpRestServer;
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
        assertEquals("a,2,true123.0111", client.allTypes("a", 2, true, (short) 1, 2L, 3.0, (byte) 'o'));
    }

    @Test
    public void testSingletonResource() throws IOException {
        Object instance = new SingletonCounterResource(2);
        tcpRestServer.addSingletonResource(instance);

        TcpRestClientFactory factory =
                new TcpRestClientFactory(Counter.class, "localhost",
                        ((SingleThreadTcpRestServer) tcpRestServer).getServerSocket().getLocalPort());

        Counter client = factory.getInstance();
        assertEquals(2, client.getCounter());

        client.increaseCounter();
        assertEquals(3, client.getCounter());

        tcpRestServer.deleteSingletonResource(instance);

        tcpRestServer.addResource(SingletonCounterResource.class);
        assertEquals(0, client.getCounter());
        client.increaseCounter();
        assertEquals(0, client.getCounter());
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
