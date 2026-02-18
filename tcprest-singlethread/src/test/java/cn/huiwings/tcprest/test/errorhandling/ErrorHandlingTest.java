package cn.huiwings.tcprest.test.errorhandling;

import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.exception.ParseException;
import cn.huiwings.tcprest.parser.DefaultRequestParser;
import cn.huiwings.tcprest.parser.RequestParser;
import cn.huiwings.tcprest.invoker.DefaultInvoker;
import cn.huiwings.tcprest.invoker.Invoker;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.server.Context;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.*;

/**
 * Tests for error handling scenarios.
 */
public class ErrorHandlingTest {

    private static final int BASE_PORT = Math.abs(new Random().nextInt()) % 10000 + 9000;
    private TcpRestServer server;
    private int port;

    @BeforeMethod
    public void setup() throws Exception {
        port = BASE_PORT + new Random().nextInt(100);
        server = new SingleThreadTcpRestServer(port);
        server.up();
        Thread.sleep(100);
    }

    @AfterMethod
    public void teardown() throws Exception {
        if (server != null) {
            server.down();
            Thread.sleep(200);
        }
    }

    @Test(expectedExceptions = NoSuchMethodException.class)
    public void testInvalidMethodNotFound() throws Exception {
        server.addResource(HelloWorldResource.class);
        RequestParser parser = new DefaultRequestParser(server);

        // Try to extract a method that doesn't exist - should throw NoSuchMethodException
        String meta = "cn.huiwings.tcprest.test.HelloWorldResource/nonExistentMethod";
        String request = "0|" + ProtocolSecurity.encodeComponent(meta) + "|" + ProtocolSecurity.encodeComponent("");
        Context ctx = parser.parse(request);
        Invoker invoker = new DefaultInvoker();
        invoker.invoke(ctx);
    }

    @Test(expectedExceptions = ParseException.class)
    public void testMalformedRequest() throws Exception {
        server.addResource(HelloWorldResource.class);
        RequestParser parser = new DefaultRequestParser(server);

        // Send a malformed request - should throw ParseException
        parser.parse("this is not a valid request format");
    }

    @Test
    public void testServerHandlesValidRequestsAfterErrors() throws Exception {
        server.addResource(HelloWorldResource.class);
        RequestParser parser = new DefaultRequestParser(server);
        Invoker invoker = new DefaultInvoker();

        // First, cause an error
        try {
            parser.parse("invalid request");
        } catch (ParseException e) {
            // Expected
        }

        // Server should still handle valid requests properly
        String meta = "cn.huiwings.tcprest.test.HelloWorldResource/helloWorld";
        String request = "0|" + ProtocolSecurity.encodeComponent(meta) + "|" + ProtocolSecurity.encodeComponent("");
        Context ctx = parser.parse(request);
        String result = (String) invoker.invoke(ctx);
        assertEquals(result, "Hello, world!");
    }
}
