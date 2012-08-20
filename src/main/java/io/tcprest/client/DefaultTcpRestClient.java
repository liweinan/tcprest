package io.tcprest.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * @author Weinan Li
 * @date 07 30 2012
 */
public class DefaultTcpRestClient implements TcpRestClient {
    private String deletgatedClassName;
    private String host;
    private int port;

    public DefaultTcpRestClient(String deletgatedClassName, String host, int port) {
        this.deletgatedClassName = deletgatedClassName;
        this.host = host;
        this.port = port;
    }

    public String sendRequest(String request, int timeout) throws IOException {
        Socket clientSocket = new Socket(host, port);

        if (timeout > 0)
            clientSocket.setSoTimeout(timeout * 1000);

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.println(request);
        writer.flush();
        String response = reader.readLine();
        clientSocket.close();
        return response;
    }

    public String getDeletgatedClassName() {
        return deletgatedClassName;
    }
}
