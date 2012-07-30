package net.bluedash.tcprest.test;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class SimpleTcpServerSmokeTests extends SmokeTestTemplate {


    @Test
    public void testSimpleClient() throws IOException {
        tcpRestServer.addResource(HelloWorldRestlet.class);

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("net.bluedash.tcprest.test.HelloWorldRestlet/helloWorld()");
        writer.flush();

        String response = reader.readLine();
        assertEquals("{{Hello, world!}}java.lang.String", response);

    }

    @Test
    public void testArgs() throws IOException {
        tcpRestServer.addResource(HelloWorldRestlet.class);

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("net.bluedash.tcprest.test.HelloWorldRestlet/sayHelloTo({{Jack!}}java.lang.String)");
        writer.flush();

        String response = reader.readLine();

        assertEquals("{{Hello, Jack!}}java.lang.String", response);

    }

    @Test
    public void testMultipleArgs() throws IOException {
        tcpRestServer.addResource(HelloWorldRestlet.class);

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println("net.bluedash.tcprest.test.HelloWorldRestlet/oneTwoThree({{One}}java.lang.String,{{2}}java.lang.Integer,{{true}}java.lang.Boolean)");
        writer.flush();

        String response = reader.readLine();
        assertEquals("{{One,2,true}}java.lang.String", response);

    }


}
