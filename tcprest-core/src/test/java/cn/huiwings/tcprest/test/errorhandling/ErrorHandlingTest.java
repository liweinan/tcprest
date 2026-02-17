package cn.huiwings.tcprest.test.errorhandling;

import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.exception.ParseException;
import cn.huiwings.tcprest.extractor.DefaultExtractor;
import cn.huiwings.tcprest.extractor.Extractor;
import cn.huiwings.tcprest.invoker.DefaultInvoker;
import cn.huiwings.tcprest.invoker.Invoker;
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
        Extractor extractor = new DefaultExtractor(server);

        // Try to extract a method that doesn't exist - should throw NoSuchMethodException
        Context ctx = extractor.extract("cn.huiwings.tcprest.test.HelloWorldResource/nonExistentMethod()");
        Invoker invoker = new DefaultInvoker();
        invoker.invoke(ctx);
    }

    @Test(expectedExceptions = ParseException.class)
    public void testMalformedRequest() throws Exception {
        server.addResource(HelloWorldResource.class);
        Extractor extractor = new DefaultExtractor(server);

        // Send a malformed request - should throw ParseException
        extractor.extract("this is not a valid request format");
    }

    @Test
    public void testServerHandlesValidRequestsAfterErrors() throws Exception {
        server.addResource(HelloWorldResource.class);
        Extractor extractor = new DefaultExtractor(server);
        Invoker invoker = new DefaultInvoker();

        // First, cause an error
        try {
            extractor.extract("invalid request");
        } catch (ParseException e) {
            // Expected
        }

        // Server should still handle valid requests properly
        Context ctx = extractor.extract("cn.huiwings.tcprest.test.HelloWorldResource/helloWorld()");
        String result = (String) invoker.invoke(ctx);
        assertEquals(result, "Hello, world!");
    }
}
