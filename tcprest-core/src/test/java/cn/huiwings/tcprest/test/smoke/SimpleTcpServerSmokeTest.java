package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.conveter.Converter;
import cn.huiwings.tcprest.conveter.DefaultConverter;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class SimpleTcpServerSmokeTest {

    protected TcpRestServer tcpRestServer;
    protected Socket clientSocket;


    @BeforeMethod
    public void startTcpRestServer() throws Exception {
        int port = Math.abs(new Random().nextInt()) % 10000 + 8000;
        tcpRestServer = new SingleThreadTcpRestServer(port);
        tcpRestServer.up();
        clientSocket = new Socket("localhost", port);
    }

    @AfterMethod
    public void stopTcpRestServer() throws IOException {
        tcpRestServer.down();
        clientSocket.close();
    }

    // Helper to strip compression prefix from response
    private String stripCompressionPrefix(String response) {
        if (response != null && (response.startsWith("0|") || response.startsWith("1|"))) {
            return response.substring(2);
        }
        return response;
    }

    @Test
    public void testSimpleClient() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);
        Converter converter = new DefaultConverter();

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("cn.huiwings.tcprest.test.HelloWorldResource/helloWorld()");
        writer.flush();

        String response = reader.readLine();
        Assert.assertEquals("Hello, world!", converter.decodeParam(stripCompressionPrefix(response)));

    }

    @Test
    public void testArgs() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);
        Converter converter = new DefaultConverter();
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("cn.huiwings.tcprest.test.HelloWorldResource/sayHelloTo(" + converter.encodeParam("Jack!") + ")");
        writer.flush();

        String response = reader.readLine();

        Assert.assertEquals("Hello, Jack!", converter.decodeParam(stripCompressionPrefix(response)));

    }

    @Test
    public void testMultipleArgs() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);
        Converter converter = new DefaultConverter();
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("cn.huiwings.tcprest.test.HelloWorldResource/oneTwoThree("
                + converter.encodeParam("One")
                + TcpRestProtocol.PATH_SEPERATOR
                + converter.encodeParam("2")
                + TcpRestProtocol.PATH_SEPERATOR
                + converter.encodeParam("true")
                + ")");
        writer.flush();

        String response = reader.readLine();
        Assert.assertEquals("One,2,true", converter.decodeParam(stripCompressionPrefix(response)));

    }


}
