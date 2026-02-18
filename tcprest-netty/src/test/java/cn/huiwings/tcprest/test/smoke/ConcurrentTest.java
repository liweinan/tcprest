package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.HelloWorld;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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

    @BeforeClass
    public void startTcpRestServer()
            throws Exception {
        tcpRestServer.up();
        // Wait for async server to be fully ready
        Thread.sleep(500);
    }

    @AfterClass
    public void stopTcpRestServer()
            throws Exception {
        tcpRestServer.down();
        // Wait for port release
        Thread.sleep(300);
    }

    @Factory
    public static Object[] create()
            throws Exception {
        List results = new ArrayList();
        results.add(new ConcurrentTest(new NettyTcpRestServer(PortGenerator.get())));
        return results.toArray();
    }


    @Test
    public void multipleClientsTest()
            throws Exception {
        final AtomicInteger counter = new AtomicInteger();

        Thread threads[] = new Thread[THRESHOLD];
        // Test with: tcpRestServer.getClass().getCanonicalName()
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
