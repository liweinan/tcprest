package net.bluedash.tcprest.test;

import net.bluedash.tcprest.exception.MapperNotFoundException;
import net.bluedash.tcprest.exception.ParseException;
import net.bluedash.tcprest.extractor.DefaultExtractor;
import net.bluedash.tcprest.extractor.Extractor;
import net.bluedash.tcprest.invoker.DefaultInvoker;
import net.bluedash.tcprest.invoker.Invoker;
import net.bluedash.tcprest.server.Context;
import net.bluedash.tcprest.server.SingleThreadTcpRestServer;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;


/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class DefaultExtractorAndDefaultInvokerSmokeTests {

    @Test
    public void testDefaultExtractAndInvoke() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, IOException, ParseException, MapperNotFoundException {
        SingleThreadTcpRestServer server = new SingleThreadTcpRestServer();
        server.addResource(HelloWorldRestlet.class);
        Extractor extractor = new DefaultExtractor(server);

        Context ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldRestlet/helloWorld()");
        assertEquals(ctx.getTargetClass(), HelloWorldRestlet.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldRestlet.class.getMethod("helloWorld"));
        Invoker invoker = new DefaultInvoker();
        String response = (String) invoker.invoke(ctx);
        assertEquals("Hello, world!", response);

        // test arguments
        ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldRestlet/sayHelloTo({{Jack!}}java.lang.String)");
        assertEquals(ctx.getTargetClass(), HelloWorldRestlet.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldRestlet.class.getMethod("sayHelloTo", String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("Hello, Jack!", response);

        // test multiple arguments
        ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldRestlet/sayHelloFromTo({{Jack}}java.lang.String,{{Lucy}}java.lang.String)");
        assertEquals(ctx.getTargetClass(), HelloWorldRestlet.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldRestlet.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("Jack say hello to Lucy", response);


        // test params with parentheses inside
        ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldRestlet/sayHelloFromTo({{(me}}java.lang.String,{{you)}}java.lang.String)");
        assertEquals(ctx.getTargetClass(), HelloWorldRestlet.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldRestlet.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("(me say hello to you)", response);

    }
}
