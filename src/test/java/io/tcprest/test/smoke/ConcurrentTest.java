package io.tcprest.test.smoke;

import io.tcprest.client.TcpRestClientFactory;
import io.tcprest.test.HelloWorld;
import io.tcprest.test.HelloWorldResource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date 08 06 2012
 */
public class ConcurrentTest extends TcpClientFactorySmokeTest {

    private TcpRestClientFactory factory;

    @Test
    public void multipleClientsTest() {
        tcpRestServer.addResource(HelloWorldResource.class);

        factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost",
                        tcpRestServer.getServerPort());

        for (int i = 0; i < 100; i++) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        HelloWorld client = (HelloWorld) factory.getInstance();
                        assertEquals("Hello, world!", client.helloWorld());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
        }
    }
}
