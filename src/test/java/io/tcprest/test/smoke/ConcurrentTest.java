package io.tcprest.test.smoke;

import io.tcprest.client.TcpRestClientFactory;
import io.tcprest.test.HelloWorld;
import io.tcprest.test.HelloWorldResource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Weinan Li
 * @date 08 06 2012
 */
public class ConcurrentTest extends TcpClientFactorySmokeTest {

    private TcpRestClientFactory factory;

    //TODO NioServer will throw exception: Connection reset
    //TODO Needs optimize
    private static final int THREAD_NUM = 100;

    @Test
    public void multipleClientsTest() throws Exception {
        long singleThreadServerUsedTime = 0;
        long nioServerUsedTime = 0;

        for (int i = 0; i < 2; i++) {
            long start = System.currentTimeMillis();
            Thread[] threads = new Thread[THREAD_NUM];
            System.out.println("-----------------------------------" + testServers[i].getClass().getCanonicalName() + "--------------------------------");
            testServers[i].addResource(HelloWorldResource.class);

            factory =
                    new TcpRestClientFactory(HelloWorld.class, "localhost",
                            testServers[i].getServerPort());

            for (int n = 0; n < THREAD_NUM; n++) {
                threads[n] = new Thread() {
                    @Override
                    public void run() {
                        HelloWorld client = (HelloWorld) factory.getInstance();
                        assertEquals("Hello, world!", client.helloWorld());
                    }
                };
                threads[n].start();
            }
            for (Thread t : threads) {
                t.join();
            }
            if (i == 0)
                singleThreadServerUsedTime = System.currentTimeMillis() - start;
            else
                nioServerUsedTime = System.currentTimeMillis() - start;

        }
        System.out.println("singleThreadServerUsedTime: " + singleThreadServerUsedTime);
        System.out.println("nioServerUsedTime: " + nioServerUsedTime);

        // NioServer should be faster than SingleThreadServer
        assertTrue((nioServerUsedTime - singleThreadServerUsedTime) < 0);

    }
}
