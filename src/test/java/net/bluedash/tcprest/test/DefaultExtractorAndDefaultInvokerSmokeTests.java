package net.bluedash.tcprest.test;

import net.bluedash.tcprest.exception.ParseException;
import net.bluedash.tcprest.extractor.DefaultExtractor;
import net.bluedash.tcprest.extractor.Extractor;
import net.bluedash.tcprest.invoker.DefaultInvoker;
import net.bluedash.tcprest.invoker.Invoker;
import net.bluedash.tcprest.server.Context;
import net.bluedash.tcprest.server.SingleThreadTcpRestServer;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class DefaultExtractorAndDefaultInvokerSmokeTests {

    @Test
    public void testDefaultExtractAndInvoke() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, IOException, ParseException {
        SingleThreadTcpRestServer server = new SingleThreadTcpRestServer();
        server.addResource(HelloWorldRestlet.class);
        Extractor extractor = new DefaultExtractor(server);

        Context ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldRestlet/helloWorld()");
        assertEquals(ctx.getTargetClass(), HelloWorldRestlet.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldRestlet.class.getMethod("helloWorld"));
        Invoker invoker = new DefaultInvoker();
        String response = (String) invoker.invoke(ctx);
        assertEquals(response, "Hello, world!");

        // test arguments
        ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldRestlet/sayHelloTo(Jack!)");
        assertEquals(ctx.getTargetClass(), HelloWorldRestlet.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldRestlet.class.getMethod("sayHelloTo", String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals(response, "Hello, Jack!");

        // test multiple arguments
        ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldRestlet/sayHelloFromTo(Jack,Lucy)");
        assertEquals(ctx.getTargetClass(), HelloWorldRestlet.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldRestlet.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals(response, "Jack say hello to Lucy");


        // test params with parentheses inside
        ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldRestlet/sayHelloFromTo((me,you))");
        assertEquals(ctx.getTargetClass(), HelloWorldRestlet.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldRestlet.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals(response, "(me say hello to you)");

    }
}
