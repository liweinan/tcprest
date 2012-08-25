package io.tcprest.client;

import io.tcprest.server.SSLParam;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.KeyStore;

/**
 * @author Weinan Li
 * @date 07 30 2012
 */
public class DefaultTcpRestClient implements TcpRestClient {
    private String deletgatedClassName;
    private String host;
    private int port;
    private SSLParam sslParam;

    public DefaultTcpRestClient(String deletgatedClassName, String host, int port) {
        this.deletgatedClassName = deletgatedClassName;
        this.host = host;
        this.port = port;
    }

    public DefaultTcpRestClient(SSLParam sslParam, String deletgatedClassName, String host, int port) {
        this.deletgatedClassName = deletgatedClassName;
        this.host = host;
        this.port = port;
        this.sslParam = sslParam;
    }

    private String sendRequest(String request, Socket socket) throws Exception {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer.println(request);
        writer.flush();
        String response = reader.readLine();
        socket.close();
        return response;
    }


    public String sendRequest(String request, int timeout) throws Exception {
        if (sslParam == null) {
            Socket clientSocket = new Socket(host, port);

            if (timeout > 0)
                clientSocket.setSoTimeout(timeout * 1000);

            return sendRequest(request, clientSocket);
        }

        // Set the key store to use for validating the server cert.
        System.setProperty("javax.net.ssl.trustStore", sslParam.getTrustStorePath());
        Socket socket = null;
        if (sslParam.isNeedClientAuth()) {
            socket = clientWithCert(sslParam, host, port, timeout);
        } else {
            socket = clientWithoutCert(host, port, timeout);
        }

        return sendRequest(request, socket);
    }

    public String getDeletgatedClassName() {
        return deletgatedClassName;
    }

    private Socket clientWithoutCert(String host, int port, int timeout) throws Exception {
        SocketFactory sf = SSLSocketFactory.getDefault();
        Socket socket = sf.createSocket(host, port);
        if (timeout > 0)
            socket.setSoTimeout(timeout);
        return socket;
    }

    private Socket clientWithCert(SSLParam sslParam, String host, int port, int timeout) throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        KeyStore ks = KeyStore.getInstance("jceks");

        ks.load(new FileInputStream(sslParam.getKeyStorePath()), null);
        KeyManagerFactory kf = KeyManagerFactory.getInstance("SunX509");
        kf.init(ks, sslParam.getKeyStoreKeyPass().toCharArray());
        context.init(kf.getKeyManagers(), null, null);

        SocketFactory factory = context.getSocketFactory();
        Socket socket = factory.createSocket(host, port);
        if (timeout > 0)
            socket.setSoTimeout(timeout);
        return socket;

    }

}
