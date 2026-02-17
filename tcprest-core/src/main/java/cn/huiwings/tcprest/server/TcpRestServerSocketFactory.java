package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.commons.PropertyProcessor;
import cn.huiwings.tcprest.ssl.SSLParam;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;

/**
 * Factory for creating server sockets with optional SSL and bind address support.
 *
 * @author Weinan Li
 * @created_at 08 25 2012
 */
public class TcpRestServerSocketFactory {

    private static final int DEFAULT_BACKLOG = 50;

    /**
     * Create server socket with optional SSL support (binds to all interfaces).
     *
     * @param port the port to bind to
     * @param sslParam SSL parameters, or null for plain socket
     * @return server socket
     * @throws Exception if socket creation fails
     */
    public static ServerSocket getServerSocket(int port, SSLParam sslParam) throws Exception {
        return getServerSocket(port, null, sslParam);
    }

    /**
     * Create server socket with optional bind address and SSL support.
     *
     * <p><b>Bind Address Behavior:</b></p>
     * <ul>
     *   <li>null or empty: Binds to all interfaces (0.0.0.0)</li>
     *   <li>"127.0.0.1" or "localhost": Binds to localhost only</li>
     *   <li>"192.168.1.100": Binds to specific IP address</li>
     *   <li>"::1": Binds to IPv6 localhost</li>
     * </ul>
     *
     * @param port the port to bind to
     * @param bindAddress the IP address to bind to, or null to bind to all interfaces
     * @param sslParam SSL parameters, or null for plain socket
     * @return server socket
     * @throws Exception if socket creation fails or address is invalid
     */
    public static ServerSocket getServerSocket(int port, String bindAddress, SSLParam sslParam) throws Exception {
        InetAddress addr = (bindAddress == null || bindAddress.isEmpty())
                ? null  // Bind to all interfaces (0.0.0.0)
                : InetAddress.getByName(bindAddress);

        if (sslParam == null) {
            return new ServerSocket(port, DEFAULT_BACKLOG, addr);
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
        ServerSocket _socket = factory.createServerSocket(port, DEFAULT_BACKLOG, addr);
        ((SSLServerSocket) _socket).setNeedClientAuth(sslParam.isNeedClientAuth());
        return _socket;
    }
}
