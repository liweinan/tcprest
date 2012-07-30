package net.bluedash.tcprest.test;

import net.bluedash.tcprest.client.TcpRestClientFactory;
import net.bluedash.tcprest.client.TcpRestClientProxy;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;

import static junit.framework.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class TcpClientFactorySmokeTests extends SmokeTestTemplate {

    @Test
    public void testClient() throws IOException {
        clientSocket.close(); // we cannot use clientSocket because factory will open one.

        tcpRestServer.addResource(HelloWorldRestlet.class);

        TcpRestClientFactory factory = new TcpRestClientFactory(HelloWorld.class, "localhost", 8001);
        HelloWorld client = (HelloWorld) factory.getInstance();
        assertEquals("Hello, world!", client.helloWorld());


    }

    @Test
    public void testProxy() throws IOException {
        clientSocket.close(); // we cannot use clientSocket because proxy will open one.

        tcpRestServer.addResource(HelloWorldRestlet.class);

        HelloWorld client = (HelloWorld) Proxy.newProxyInstance(HelloWorld.class.getClassLoader(),
                new Class[]{HelloWorld.class}, new TcpRestClientProxy(HelloWorld.class.getCanonicalName(), "localhost", 8001));

        assertEquals("Hello, world!", client.helloWorld());
        assertEquals("x,2,false", client.oneTwoThree("x", 2, false));
    }


}
