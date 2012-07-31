package net.bluedash.tcprest.test;

import net.bluedash.tcprest.exception.MapperNotFoundException;
import net.bluedash.tcprest.exception.ParseException;
import net.bluedash.tcprest.extractor.DefaultExtractor;
import net.bluedash.tcprest.extractor.Extractor;
import net.bluedash.tcprest.invoker.DefaultInvoker;
import net.bluedash.tcprest.invoker.Invoker;
import net.bluedash.tcprest.protocol.DefaultTcpRestProtocol;
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
public class DefaultExtractorAndDefaultInvokerSmokeTest {

    @Test
    public void testDefaultExtractAndInvoke() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, IOException, ParseException, MapperNotFoundException {
        SingleThreadTcpRestServer server = new SingleThreadTcpRestServer();
        server.addResource(HelloWorldResource.class);
        Extractor extractor = new DefaultExtractor(server);

        Context ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldResource/helloWorld()");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("helloWorld"));
        Invoker invoker = new DefaultInvoker();
        String response = (String) invoker.invoke(ctx);
        assertEquals("Hello, world!", response);

        // test arguments
        ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldResource/sayHelloTo({{Jack!}}java.lang.String)");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloTo", String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("Hello, Jack!", response);

        // test multiple arguments
        ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldResource/sayHelloFromTo({{Jack}}java.lang.String"
                + DefaultTcpRestProtocol.PATH_SEPERATOR + "{{Lucy}}java.lang.String)");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("Jack say hello to Lucy", response);


        // test params with parentheses inside
        ctx = extractor.extract("net.bluedash.tcprest.test.HelloWorldResource/sayHelloFromTo({{(me}}java.lang.String"
                + DefaultTcpRestProtocol.PATH_SEPERATOR + "{{you)}}java.lang.String)");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("(me say hello to you)", response);

    }
}
