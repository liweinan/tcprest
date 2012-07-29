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
*
* @author Weinan Li
* CREATED AT: Jul 29 2012
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
