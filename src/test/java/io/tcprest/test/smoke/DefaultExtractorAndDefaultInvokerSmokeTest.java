package io.tcprest.test.smoke;

import io.tcprest.conveter.Converter;
import io.tcprest.conveter.DefaultConverter;
import io.tcprest.extractor.DefaultExtractor;
import io.tcprest.extractor.Extractor;
import io.tcprest.invoker.DefaultInvoker;
import io.tcprest.invoker.Invoker;
import io.tcprest.protocol.TcpRestProtocol;
import io.tcprest.server.Context;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.test.HelloWorldResource;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;


/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class DefaultExtractorAndDefaultInvokerSmokeTest {

    @Test
    public void testDefaultExtractAndInvoke() throws Exception {
        SingleThreadTcpRestServer server = new SingleThreadTcpRestServer();
        server.addResource(HelloWorldResource.class);
        Extractor extractor = new DefaultExtractor(server);

        Context ctx = extractor.extract("io.tcprest.test.HelloWorldResource/helloWorld()");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("helloWorld"));
        Invoker invoker = new DefaultInvoker();
        String response = (String) invoker.invoke(ctx);
        assertEquals("Hello, world!", response);

        Converter converter = new DefaultConverter();
        // test arguments
        ctx = extractor.extract("io.tcprest.test.HelloWorldResource/sayHelloTo(" + converter.encodeParam("Jack!") + ")");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloTo", String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("Hello, Jack!", response);

        // test multiple arguments
        ctx = extractor.extract("io.tcprest.test.HelloWorldResource/sayHelloFromTo(" + converter.encodeParam("Jack") + ""
                + TcpRestProtocol.PATH_SEPERATOR + converter.encodeParam("Lucy") + ")");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("Jack say hello to Lucy", response);


        // test params with parentheses inside
        ctx = extractor.extract("io.tcprest.test.HelloWorldResource/sayHelloFromTo(" + converter.encodeParam("(me")
                + TcpRestProtocol.PATH_SEPERATOR + converter.encodeParam("you)") + ")");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("(me say hello to you)", response);

    }
}
