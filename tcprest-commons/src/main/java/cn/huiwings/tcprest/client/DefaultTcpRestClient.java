package cn.huiwings.tcprest.client;

import cn.huiwings.tcprest.commons.PropertyProcessor;
import cn.huiwings.tcprest.ssl.SSLParams;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
    private SSLParams sslParams;

    public DefaultTcpRestClient(SSLParams sslParams, String deletgatedClassName, String host, int port) {
        this.deletgatedClassName = deletgatedClassName;
        this.host = host;
        this.port = port;
        this.sslParams = sslParams;
    }

    private String sendRequest(String request, Socket socket) throws Exception {
        try (PrintWriter writer = new PrintWriter(socket.getOutputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            writer.println(request);
            writer.flush();
            return reader.readLine();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public String sendRequest(String request, int timeout) throws Exception {
        if (sslParams == null) {
            Socket clientSocket = new Socket(host, port);

            if (timeout > 0)
                clientSocket.setSoTimeout(timeout * 1000);

            return sendRequest(request, clientSocket);
        }

        // Set the key store to use for validating the server cert.
        System.setProperty("javax.net.ssl.trustStore", PropertyProcessor.getFilePath(sslParams.getTrustStorePath()));
        Socket socket = null;
        if (sslParams.isNeedClientAuth()) {
            socket = sslClientWithCert(sslParams, host, port, timeout);
        } else {
            socket = sslClientWithoutCert(host, port, timeout);
        }

        return sendRequest(request, socket);
    }

    @Override
    public String getDeletgatedClassName() {
        return deletgatedClassName;
    }

    private Socket sslClientWithoutCert(String host, int port, int timeout) throws Exception {
        SocketFactory sf = SSLSocketFactory.getDefault();
        Socket socket = sf.createSocket(host, port);
        if (timeout > 0)
            socket.setSoTimeout(timeout);
        return socket;
    }

    private Socket sslClientWithCert(SSLParams sslParams, String host, int port, int timeout) throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        KeyStore ks = KeyStore.getInstance("jceks");

        FileInputStream fi = PropertyProcessor.getFileInputStream(sslParams.getKeyStorePath());
        ks.load(fi, null);
        KeyManagerFactory kf = KeyManagerFactory.getInstance("SunX509");
        kf.init(ks, sslParams.getKeyStoreKeyPass().toCharArray());
        context.init(kf.getKeyManagers(), null, null);

        SocketFactory factory = context.getSocketFactory();
        Socket socket = factory.createSocket(host, port);
        if (timeout > 0)
            socket.setSoTimeout(timeout);
        return socket;

    }

}
