package io.tcprest.test.smoke;

import io.tcprest.conveter.Converter;
import io.tcprest.conveter.DefaultConverter;
import io.tcprest.protocol.TcpRestProtocol;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.server.TcpRestServer;
import io.tcprest.test.HelloWorldResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class SimpleTcpServerSmokeTest {

    protected TcpRestServer tcpRestServer;
    protected Socket clientSocket;


    @Before
    public void startTcpRestServer() throws IOException {
        int port = Math.abs(new Random().nextInt()) % 10000 + 8000;
        tcpRestServer = new SingleThreadTcpRestServer(port);
        tcpRestServer.up();
        clientSocket = new Socket("localhost", port);
    }

    @After
    public void stopTcpRestServer() throws IOException {
        tcpRestServer.down();
        clientSocket.close();
    }

    @Test
    public void testSimpleClient() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);
        Converter converter = new DefaultConverter();

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("io.tcprest.test.HelloWorldResource/helloWorld()");
        writer.flush();

        String response = reader.readLine();
        assertEquals("Hello, world!", converter.decodeParam(response));

    }

    @Test
    public void testArgs() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);
        Converter converter = new DefaultConverter();
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("io.tcprest.test.HelloWorldResource/sayHelloTo(" + converter.encodeParam("Jack!") + ")");
        writer.flush();

        String response = reader.readLine();

        assertEquals("Hello, Jack!", converter.decodeParam(response));

    }

    @Test
    public void testMultipleArgs() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);
        Converter converter = new DefaultConverter();
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("io.tcprest.test.HelloWorldResource/oneTwoThree("
                + converter.encodeParam("One")
                + TcpRestProtocol.PATH_SEPERATOR
                + converter.encodeParam("2")
                + TcpRestProtocol.PATH_SEPERATOR
                + converter.encodeParam("true")
                + ")");
        writer.flush();

        String response = reader.readLine();
        assertEquals("One,2,true", converter.decodeParam(response));

    }


}
