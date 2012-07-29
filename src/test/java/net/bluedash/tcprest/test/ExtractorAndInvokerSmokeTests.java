package net.bluedash.tcprest.test;

import net.bluedash.tcprest.extractor.DefaultExtractor;
import net.bluedash.tcprest.extractor.Extractor;
import net.bluedash.tcprest.invoker.DefaultInvoker;
import net.bluedash.tcprest.invoker.Invoker;
import net.bluedash.tcprest.server.Context;
import net.bluedash.tcprest.server.SimpleTcpRestServer;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: weli
 * Date: 7/29/12
 * Time: 6:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtractorAndInvokerSmokeTests {

    @Test
    public void testDefaultExtractAndInvoke() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, IOException {
        SimpleTcpRestServer server = new SimpleTcpRestServer();
        server.addResource(HelloWorldRestlet.class);
        Extractor extractor = new DefaultExtractor(server);
        Context ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldRestlet/helloWorld");
        assertEquals(ctx.getTargetClass(), HelloWorldRestlet.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldRestlet.class.getMethod("helloWorld"));
        Invoker invoker = new DefaultInvoker();
        String response = invoker.invoke(ctx);
        assertEquals(response, "Hello, world!");
    }
}
