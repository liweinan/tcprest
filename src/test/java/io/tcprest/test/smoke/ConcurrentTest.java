package io.tcprest.test.smoke;

import io.tcprest.client.TcpRestClientFactory;
import io.tcprest.server.NioTcpRestServer;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.server.TcpRestServer;
import io.tcprest.test.HelloWorld;
import io.tcprest.test.HelloWorldResource;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Weinan Li
 * @date 08 06 2012
 */
public class ConcurrentTest {

    public ConcurrentTest(TcpRestServer tcpRestServer) {
        this.tcpRestServer = tcpRestServer;
    }

    @BeforeTest
    public void startTcpRestServer()
            throws Exception {
        tcpRestServer.up();
    }

    @AfterTest
    public void stopTcpRestServer()
            throws Exception {
        tcpRestServer.down();
    }

    @Factory
    public static Object[] create()
            throws Exception {
        List result = new ArrayList();
        result.add(new ConcurrentTest(new SingleThreadTcpRestServer(Math.abs((new Random()).nextInt()) % 10000 + 8000)));
        result.add(new ConcurrentTest(new NioTcpRestServer(Math.abs((new Random()).nextInt()) % 10000 + 8000)));
        return result.toArray();
    }

    @Test
    public void multipleClientsTest()
            throws Exception {
        Thread threads[] = new Thread[100];
        System.out.println((new StringBuilder()).append("-----------------------------------").append(tcpRestServer.getClass().getCanonicalName()).append("--------------------------------").toString());
        tcpRestServer.addResource(HelloWorldResource.class);
        factory = new TcpRestClientFactory(HelloWorld.class, "localhost", tcpRestServer.getServerPort());
        for (int n = 0; n < 100; n++) {
            threads[n] = new Thread() {
                public void run() {
                    HelloWorld client = (HelloWorld) factory.getInstance();
                    Assert.assertEquals("Hello, world!", client.helloWorld());
                }
            };

            threads[n].start();
        }

        for (int n = 0; n < 100; n++) {
            threads[n].join();
        }

    }

    private TcpRestClientFactory factory;
    private static final int THREAD_NUM = 100;
    protected TcpRestServer tcpRestServer;

}
