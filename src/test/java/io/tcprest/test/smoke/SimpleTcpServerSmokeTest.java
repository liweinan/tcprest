package io.tcprest.test.smoke;

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

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("io.tcprest.test.HelloWorldResource/helloWorld()");
        writer.flush();

        String response = reader.readLine();
        assertEquals("{{Hello, world!}}java.lang.String", response);

    }

    @Test
    public void testArgs() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("io.tcprest.test.HelloWorldResource/sayHelloTo({{Jack!}}java.lang.String)");
        writer.flush();

        String response = reader.readLine();

        assertEquals("{{Hello, Jack!}}java.lang.String", response);

    }

    @Test
    public void testMultipleArgs() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("io.tcprest.test.HelloWorldResource/oneTwoThree({{One}}java.lang.String"
                + TcpRestProtocol.PATH_SEPERATOR
                + "{{2}}java.lang.Integer"
                + TcpRestProtocol.PATH_SEPERATOR + "{{true}}java.lang.Boolean)");
        writer.flush();

        String response = reader.readLine();
        assertEquals("{{One,2,true}}java.lang.String", response);

    }


}
