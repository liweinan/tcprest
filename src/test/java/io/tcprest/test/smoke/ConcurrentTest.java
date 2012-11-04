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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Weinan Li
 * @date 08 06 2012
 */
public class ConcurrentTest {

    public static final int THRESHOLD = 10;
    private TcpRestClientFactory factory;
    protected TcpRestServer tcpRestServer;

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
        List results = new ArrayList();
        results.add(new ConcurrentTest(new SingleThreadTcpRestServer(PortGenerator.get())));
        Thread.sleep(5 * THRESHOLD);
        results.add(new ConcurrentTest(new NioTcpRestServer(PortGenerator.get())));
//        results.add(new ConcurrentTest(new NettyTcpRestServer(PortGenerator.get())));
        return results.toArray();
    }


    @Test
    public void multipleClientsTest()
            throws Exception {
        final AtomicInteger counter = new AtomicInteger();

        Thread threads[] = new Thread[THRESHOLD];
        System.out.println((new StringBuilder()).append("-----------------------------------").append(tcpRestServer.getClass().getCanonicalName()).append("--------------------------------").toString());
        tcpRestServer.addResource(HelloWorldResource.class);
        factory = new TcpRestClientFactory(HelloWorld.class, "localhost", tcpRestServer.getServerPort());
        for (int n = 0; n < THRESHOLD; n++) {
            Thread.sleep(5);
            threads[n] = new Thread() {
                public void run() {
                    HelloWorld client = (HelloWorld) factory.getInstance();
                    Assert.assertEquals("Hello, world!", client.helloWorld());
                    counter.incrementAndGet();
                }
            };

            threads[n].start();
        }

        for (int n = 0; n < THRESHOLD; n++) {
            threads[n].join();
        }

        Assert.assertEquals(counter.get(), THRESHOLD);

    }


}
