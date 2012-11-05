package io.tcprest.test.smoke;

import io.tcprest.client.TcpRestClientFactory;
import io.tcprest.client.TcpRestClientProxy;
import io.tcprest.server.NioTcpRestServer;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.server.TcpRestServer;
import io.tcprest.test.Counter;
import io.tcprest.test.HelloWorld;
import io.tcprest.test.HelloWorldResource;
import io.tcprest.test.SingletonCounterResource;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class TcpClientFactorySmokeTest {
    protected TcpRestServer tcpRestServer;

    public TcpClientFactorySmokeTest(TcpRestServer tcpRestServer) {
        this.tcpRestServer = tcpRestServer;
    }

    @Factory
    public static Object[] create()
            throws Exception {
        List result = new ArrayList();
        result.add(new TcpClientFactorySmokeTest(new SingleThreadTcpRestServer(PortGenerator.get())));
        result.add(new TcpClientFactorySmokeTest(new NioTcpRestServer(PortGenerator.get())));
        // TODO Netty Server still not pass largeDataTest()
//        result.add(new TcpClientFactorySmokeTest(new NettyTcpRestServer(PortGenerator.get())));
        return result.toArray();
    }

    @BeforeTest
    public void startTcpRestServer() throws Exception {
        tcpRestServer.up();
    }

    @AfterTest
    public void stopTcpRestServer() throws Exception {
        tcpRestServer.down();
    }


    @Test
    public void testClient() throws Exception {

        System.out.println("-----------------------------------" + tcpRestServer.getClass().getCanonicalName() + "--------------------------------");
        tcpRestServer.addResource(HelloWorldResource.class);

        TcpRestClientFactory factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost",
                        tcpRestServer.getServerPort());

        HelloWorld client = (HelloWorld) factory.getInstance();

        assertEquals("Hello, world!", client.helloWorld());
        assertEquals("a,2,true123.0111", client.allTypes("a", 2, true, (short) 1, 2L, 3.0, (byte) 'o'));
    }


    @Test(expectedExceptions = Exception.class)
    public void clientSideTimeoutTest() {
        System.out.println("-----------------------------------" + tcpRestServer.getClass().getCanonicalName() + "--------------------------------");

        tcpRestServer.addResource(HelloWorldResource.class);

        TcpRestClientFactory factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost",
                        tcpRestServer.getServerPort());

        HelloWorld client = (HelloWorld) factory.getInstance();
        client.timeout();

    }


    @Test
    public void testSingletonResource() throws Exception {
        System.out.println("-----------------------------------" + tcpRestServer.getClass().getCanonicalName() + "--------------------------------");

        Object instance = new SingletonCounterResource(2);
        tcpRestServer.addSingletonResource(instance);

        TcpRestClientFactory factory =
                new TcpRestClientFactory(Counter.class, "localhost",
                        tcpRestServer.getServerPort());

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
    public void testProxy() throws Exception {
        System.out.println("-----------------------------------" + tcpRestServer.getClass().getCanonicalName() + "--------------------------------");

        tcpRestServer.addResource(HelloWorldResource.class);

        HelloWorld client = (HelloWorld) Proxy.newProxyInstance(HelloWorld.class.getClassLoader(),
                new Class[]{HelloWorld.class}, new TcpRestClientProxy(HelloWorld.class.getCanonicalName(), "localhost",
                tcpRestServer.getServerPort()));

        assertEquals("Hello, world!", client.helloWorld());
        assertEquals("x,2,false", client.oneTwoThree("x", 2, false));

    }

    private interface NullParam {
        public String nullMethod(String one, String empty, String two);
    }

    public class NullParamResource implements NullParam {
        public String nullMethod(String one, String empty, String two) {
            if (empty == null) {
                return one + two;
            } else {
                return one + empty + two;
            }
        }

    }

    @Test
    public void testNullParameter() {

        System.out.println("-----------------------------------" + tcpRestServer.getClass().getCanonicalName() + "--------------------------------");

        tcpRestServer.addSingletonResource(new NullParamResource());

        TcpRestClientFactory factory =
                new TcpRestClientFactory(NullParam.class, "localhost",
                        tcpRestServer.getServerPort());

        NullParam client = factory.getInstance();


        assertEquals("onetwo", client.nullMethod("one", null, "two"));

    }

    @Test
    public void testArray() {
        System.out.println("-----------------------------------" + tcpRestServer.getClass().getCanonicalName() + "--------------------------------");
        tcpRestServer.addSingletonResource(new HelloWorldResource());

        HelloWorld client = (HelloWorld) Proxy.newProxyInstance(HelloWorld.class.getClassLoader(),
                new Class[]{HelloWorld.class}, new TcpRestClientProxy(HelloWorld.class.getCanonicalName(), "localhost",
                tcpRestServer.getServerPort()));

        String[] in = new String[]{"a", "b", "c"};
        String[] out = client.getArray(in);
        for (int i = 0; i < in.length; i++) {
            assertEquals(in[i], out[i]);
        }
    }


    @Test
    public void largeDataTest() {
        StringBuilder builder = new StringBuilder();
        String[] alpha = {"a", "b", "c", "d", "e", "f"};
        for (int i = 0; i < 1024 * 10; i++) {
            builder.append(alpha[i % alpha.length]);
        }
        String req = builder.toString();

        tcpRestServer.addSingletonResource(new HelloWorldResource());
        System.out.println("-----------------------------------" + tcpRestServer.getClass().getCanonicalName() + "--------------------------------");
        TcpRestClientFactory factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost",
                        tcpRestServer.getServerPort());

        HelloWorld client = (HelloWorld) factory.getInstance();
        assertEquals(req, client.echo(req));

    }
}
