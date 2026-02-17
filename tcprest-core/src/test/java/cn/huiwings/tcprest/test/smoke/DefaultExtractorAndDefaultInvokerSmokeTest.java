package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.conveter.Converter;
import cn.huiwings.tcprest.conveter.DefaultConverter;
import cn.huiwings.tcprest.extractor.DefaultExtractor;
import cn.huiwings.tcprest.extractor.Extractor;
import cn.huiwings.tcprest.invoker.DefaultInvoker;
import cn.huiwings.tcprest.invoker.Invoker;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.server.Context;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


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

        Context ctx = extractor.extract("cn.huiwings.tcprest.test.HelloWorldResource/helloWorld()");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("helloWorld"));
        Invoker invoker = new DefaultInvoker();
        String response = (String) invoker.invoke(ctx);
        assertEquals("Hello, world!", response);

        Converter converter = new DefaultConverter();
        // test arguments
        ctx = extractor.extract("cn.huiwings.tcprest.test.HelloWorldResource/sayHelloTo(" + converter.encodeParam("Jack!") + ")");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloTo", String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("Hello, Jack!", response);

        // test multiple arguments
        ctx = extractor.extract("cn.huiwings.tcprest.test.HelloWorldResource/sayHelloFromTo(" + converter.encodeParam("Jack") + ""
                + TcpRestProtocol.PATH_SEPERATOR + converter.encodeParam("Lucy") + ")");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("Jack say hello to Lucy", response);


        // test params with parentheses inside
        ctx = extractor.extract("cn.huiwings.tcprest.test.HelloWorldResource/sayHelloFromTo(" + converter.encodeParam("(me")
                + TcpRestProtocol.PATH_SEPERATOR + converter.encodeParam("you)") + ")");
        assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
        assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloFromTo", String.class, String.class));
        assertNotNull(ctx.getParams());
        invoker = new DefaultInvoker();
        response = (String) invoker.invoke(ctx);
        assertEquals("(me say hello to you)", response);

    }
}
