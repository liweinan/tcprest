package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.conveter.Converter;
import cn.huiwings.tcprest.conveter.DefaultConverter;
import cn.huiwings.tcprest.extractor.DefaultExtractor;
import cn.huiwings.tcprest.extractor.Extractor;
import cn.huiwings.tcprest.invoker.DefaultInvoker;
import cn.huiwings.tcprest.invoker.Invoker;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.security.ProtocolSecurity;
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
        try {
            server.addResource(HelloWorldResource.class);
            Extractor extractor = new DefaultExtractor(server);
            Converter converter = new DefaultConverter();

            // Test 1: No arguments - helloWorld()
            String meta1 = "cn.huiwings.tcprest.test.HelloWorldResource/helloWorld";
            String request1 = "0|" + ProtocolSecurity.encodeComponent(meta1) + "|" + ProtocolSecurity.encodeComponent("");
            Context ctx = extractor.extract(request1);
            assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
            assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("helloWorld"));
            Invoker invoker = new DefaultInvoker();
            String response = (String) invoker.invoke(ctx);
            assertEquals("Hello, world!", response);

            // Test 2: Single argument - sayHelloTo(Jack!)
            String meta2 = "cn.huiwings.tcprest.test.HelloWorldResource/sayHelloTo";
            String params2 = converter.encodeParam("Jack!");
            String request2 = "0|" + ProtocolSecurity.encodeComponent(meta2) + "|" + ProtocolSecurity.encodeComponent(params2);
            ctx = extractor.extract(request2);
            assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
            assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloTo", String.class));
            assertNotNull(ctx.getParams());
            invoker = new DefaultInvoker();
            response = (String) invoker.invoke(ctx);
            assertEquals("Hello, Jack!", response);

            // Test 3: Multiple arguments - sayHelloFromTo(Jack, Lucy)
            String meta3 = "cn.huiwings.tcprest.test.HelloWorldResource/sayHelloFromTo";
            String params3 = converter.encodeParam("Jack") + TcpRestProtocol.PARAM_SEPARATOR + converter.encodeParam("Lucy");
            String request3 = "0|" + ProtocolSecurity.encodeComponent(meta3) + "|" + ProtocolSecurity.encodeComponent(params3);
            ctx = extractor.extract(request3);
            assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
            assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloFromTo", String.class, String.class));
            assertNotNull(ctx.getParams());
            invoker = new DefaultInvoker();
            response = (String) invoker.invoke(ctx);
            assertEquals("Jack say hello to Lucy", response);

            // Test 4: Params with parentheses - sayHelloFromTo((me, you))
            String meta4 = "cn.huiwings.tcprest.test.HelloWorldResource/sayHelloFromTo";
            String params4 = converter.encodeParam("(me") + TcpRestProtocol.PARAM_SEPARATOR + converter.encodeParam("you)");
            String request4 = "0|" + ProtocolSecurity.encodeComponent(meta4) + "|" + ProtocolSecurity.encodeComponent(params4);
            ctx = extractor.extract(request4);
            assertEquals(ctx.getTargetClass(), HelloWorldResource.class);
            assertEquals(ctx.getTargetMethod(), HelloWorldResource.class.getMethod("sayHelloFromTo", String.class, String.class));
            assertNotNull(ctx.getParams());
            invoker = new DefaultInvoker();
            response = (String) invoker.invoke(ctx);
            assertEquals("(me say hello to you)", response);
        } finally {
            // Release the server port even though server was never started
            server.down();
        }
    }
}
