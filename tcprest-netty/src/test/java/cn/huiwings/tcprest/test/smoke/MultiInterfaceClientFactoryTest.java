package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.Counter;
import cn.huiwings.tcprest.test.HelloWorld;
import cn.huiwings.tcprest.test.HelloWorldResource;
import cn.huiwings.tcprest.test.SingletonCounterResource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Tests TcpRestClientFactory with multiple interfaces registered:
 * getInstance(Class), getClient(Class), and behavior of getInstance() when multiple interfaces are set.
 */
public class MultiInterfaceClientFactoryTest {

    private TcpRestServer tcpRestServer;

    @BeforeClass
    public void startTcpRestServer() throws Exception {
        tcpRestServer = new NettyTcpRestServer(PortGenerator.get());
        tcpRestServer.up();
        Thread.sleep(500);

        tcpRestServer.addResource(HelloWorldResource.class);
        tcpRestServer.addSingletonResource(new SingletonCounterResource(0));
    }

    @AfterClass
    public void stopTcpRestServer() throws Exception {
        if (tcpRestServer != null) {
            tcpRestServer.down();
            Thread.sleep(300);
        }
    }

    @Test
    public void getInstanceByType_returnsCorrectProxies() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
                new Class<?>[]{HelloWorld.class, Counter.class},
                "localhost",
                tcpRestServer.getServerPort());

        HelloWorld helloClient = factory.getInstance(HelloWorld.class);
        Counter counterClient = factory.getInstance(Counter.class);

        assertEquals(helloClient.helloWorld(), "Hello, world!");
        assertEquals(counterClient.getCounter(), 0);
        counterClient.increaseCounter();
        assertEquals(counterClient.getCounter(), 1);
    }

    @Test
    public void getClientByType_returnsSameAsGetInstanceByType() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
                new Class<?>[]{HelloWorld.class, Counter.class},
                "localhost",
                tcpRestServer.getServerPort());

        HelloWorld viaGetInstance = factory.getInstance(HelloWorld.class);
        HelloWorld viaGetClient = factory.getClient(HelloWorld.class);

        assertEquals(viaGetInstance.helloWorld(), "Hello, world!");
        assertEquals(viaGetClient.helloWorld(), "Hello, world!");
    }

    @Test
    public void getInstanceWithNoArgs_throwsWhenMultipleInterfaces() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
                new Class<?>[]{HelloWorld.class, Counter.class},
                "localhost",
                tcpRestServer.getServerPort());

        try {
            factory.getInstance();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals(e.getMessage(), "Multiple interfaces registered; use getInstance(Class<T>) to get a proxy.");
        }
    }

    @Test
    public void getInstanceWithUnregisteredType_throws() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
                new Class<?>[]{HelloWorld.class, Counter.class},
                "localhost",
                tcpRestServer.getServerPort());

        try {
            factory.getInstance(UnregisteredInterface.class);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Interface not registered with this factory: " + UnregisteredInterface.class.getName());
        }
    }

    @Test
    public void singleInterfaceConstructor_rejectsConcreteClass() {
        try {
            new TcpRestClientFactory(HelloWorldResource.class, "localhost", tcpRestServer.getServerPort());
            fail("Expected IllegalArgumentException when registering a class");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "resourceClass must be an interface: " + HelloWorldResource.class.getName());
        }
    }

    @Test
    public void multiInterfaceConstructor_rejectsConcreteClass() {
        try {
            new TcpRestClientFactory(
                    new Class<?>[]{HelloWorld.class, HelloWorldResource.class},
                    "localhost",
                    tcpRestServer.getServerPort());
            fail("Expected IllegalArgumentException when registering a class");
        } catch (IllegalArgumentException e) {
            assert e.getMessage() != null && e.getMessage().contains("must be an interface")
                    && e.getMessage().contains(HelloWorldResource.class.getName());
        }
    }

    private interface UnregisteredInterface {
        void foo();
    }
}
