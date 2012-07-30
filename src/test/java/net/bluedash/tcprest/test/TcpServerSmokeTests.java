package net.bluedash.tcprest.test;

import net.bluedash.tcprest.server.SingleThreadTcpRestServer;
import net.bluedash.tcprest.server.TcpRestServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class TcpServerSmokeTests {

    private TcpRestServer tcpRestServer;
    private Socket clientSocket;


    @Before
    public void startTcpRestServer() throws IOException {
        tcpRestServer = new SingleThreadTcpRestServer(8001);
        tcpRestServer.up();
        clientSocket = new Socket("localhost", 8001);
    }

    @After
    public void stopTcpRestServer() throws IOException {
        tcpRestServer.down();
        clientSocket.close();
    }

    @Test
    public void testSimpleClient() throws IOException {
        tcpRestServer.addResource(HelloWorldRestlet.class);

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("net.bluedash.tcprest.test.HelloWorldRestlet/helloWorld()");
        writer.flush();

        String response = reader.readLine();
        assertEquals("Hello, world!", response);

    }

    @Test
    public void argTest() throws IOException {
        tcpRestServer.addResource(HelloWorldRestlet.class);

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("net.bluedash.tcprest.test.HelloWorldRestlet/sayHelloTo(Jack!)");
        writer.flush();

        String response = reader.readLine();
        assertEquals("Hello, Jack!", response);

    }


}
