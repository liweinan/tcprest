package net.bluedash.tcprest.test;

import net.bluedash.tcprest.server.SingleThreadTcpRestServer;
import net.bluedash.tcprest.server.TcpRestServer;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.net.Socket;

/**
 * @author Weinan Li
 */
public class SmokeTestTemplate {

    protected TcpRestServer tcpRestServer;
    protected Socket clientSocket;


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



}
