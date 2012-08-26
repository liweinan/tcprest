package io.tcprest.server;

import io.tcprest.commons.PropertyProcessor;
import io.tcprest.ssl.SSLParam;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocket;
import java.net.ServerSocket;
import java.security.KeyStore;

/**
 * @author Weinan Li
 * @created_at 08 25 2012
 */
public class TcpRestServerSocketFactory {

    public static ServerSocket getServerSocket(int port, SSLParam sslParam) throws Exception {
        if (sslParam == null) {
            return new ServerSocket(port);
        }

//        System.setProperty("javax.net.debug", "ssl,handshake");
        System.setProperty("javax.net.ssl.trustStore", PropertyProcessor.getFilePath(sslParam.getTrustStorePath()));
        javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance("TLS");

        KeyStore ks = KeyStore.getInstance("jceks");
        ks.load(PropertyProcessor.getFileInputStream(sslParam.getKeyStorePath()), null);
        KeyManagerFactory kf = KeyManagerFactory.getInstance("SunX509");
        kf.init(ks, sslParam.getKeyStoreKeyPass().toCharArray());

        context.init(kf.getKeyManagers(), null, null);

        ServerSocketFactory factory = context.getServerSocketFactory();
        ServerSocket _socket = factory.createServerSocket(port);
        ((SSLServerSocket) _socket).setNeedClientAuth(sslParam.isNeedClientAuth());
        return _socket;
    }
}
