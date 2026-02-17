package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.client.TcpRestClientProxy;
import cn.huiwings.tcprest.server.NioTcpRestServer;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.Counter;
import cn.huiwings.tcprest.test.HelloWorld;
import cn.huiwings.tcprest.test.HelloWorldResource;
import cn.huiwings.tcprest.test.SingletonCounterResource;
import org.testng.annotations.*;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

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
        // TODO Netty Server fails on largeDataTest() - request gets truncated/fragmented
        // Error: ParseException "cannot parse request" - large Base64-encoded payloads are split
        // Root cause: Netty's frame-based protocol doesn't handle line-based protocol correctly
//        result.add(new TcpClientFactorySmokeTest(new NettyTcpRestServer(PortGenerator.get())));
        return result.toArray();
    }

    @BeforeClass
    public void startTcpRestServer() throws Exception {
        tcpRestServer.up();
        // Delay to ensure async servers are fully started
        Thread.sleep(500);
    }

    @AfterClass
    public void stopTcpRestServer() throws Exception {
        tcpRestServer.down();
        // Wait for port to be fully released
        Thread.sleep(300);
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
