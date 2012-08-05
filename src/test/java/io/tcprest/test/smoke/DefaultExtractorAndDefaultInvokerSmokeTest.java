package io.tcprest.test.smoke;

import io.tcprest.exception.MapperNotFoundException;
import io.tcprest.exception.ParseException;
import io.tcprest.extractor.DefaultExtractor;
import io.tcprest.extractor.Extractor;
import io.tcprest.invoker.DefaultInvoker;
import io.tcprest.invoker.Invoker;
import io.tcprest.protocol.TcpRestProtocol;
import io.tcprest.server.Context;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.test.HelloWorldResource;
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

        Context ctx = extractor.extract("io.tcprest.test.HelloWorldResource/helloWorld()");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("helloWorld"));
        Invoker invoker = new DefaultInvoker();
        String response = (String) invoker.invoke(ctx);
        assertEquals("Hello, world!", response);

        // test arguments
        ctx = extractor.extract("io.tcprest.test.HelloWorldResource/sayHelloTo({{Jack!}}java.lang.String)");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloTo", String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("Hello, Jack!", response);

        // test multiple arguments
        ctx = extractor.extract("io.tcprest.test.HelloWorldResource/sayHelloFromTo({{Jack}}java.lang.String"
                + TcpRestProtocol.PATH_SEPERATOR + "{{Lucy}}java.lang.String)");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("Jack say hello to Lucy", response);


        // test params with parentheses inside
        ctx = extractor.extract("io.tcprest.test.HelloWorldResource/sayHelloFromTo({{(me}}java.lang.String"
                + TcpRestProtocol.PATH_SEPERATOR + "{{you)}}java.lang.String)");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("(me say hello to you)", response);

    }
}
