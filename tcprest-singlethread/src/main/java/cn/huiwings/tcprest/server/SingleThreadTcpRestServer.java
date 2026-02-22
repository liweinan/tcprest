package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.ssl.SSLParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * SingleThreadTcpRestServer uses a single threaded Socket Server to serve the clients.
 *
 * <p><b>SSL Support:</b> This server supports SSL/TLS via {@link SSLParams}.</p>
 *
 * <p><b>Bind Address Support:</b> Supports binding to specific IP addresses for security and multi-homing.</p>
 *
 * <p><b>Use cases:</b></p>
 * <ul>
 *   <li>Development and testing</li>
 *   <li>Low-traffic applications</li>
 *   <li>Applications requiring SSL without external dependencies</li>
 *   <li>Applications requiring binding to specific network interfaces</li>
 * </ul>
 *
 * <p><b>Performance:</b> Single-threaded, handles one request at a time.
 * For high-concurrency scenarios, use {@link NioTcpRestServer} (without SSL)
 * or {@code NettyTcpRestServer} (with SSL).</p>
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class SingleThreadTcpRestServer extends AbstractTcpRestServer {


    protected ServerSocket serverSocket;
    private volatile Thread serverThread;

    /**
     * Create server on default port (8000) binding to all interfaces.
     *
     * @throws Exception if server creation fails
     */
    public SingleThreadTcpRestServer() throws Exception {
        this(TcpRestServerConfig.DEFAULT_PORT);
    }

    /**
     * Create server on specified port binding to all interfaces.
     *
     * @param port the port to bind to
     * @throws Exception if server creation fails
     */
    public SingleThreadTcpRestServer(int port) throws Exception {
        this(TcpRestServerSocketFactory.getServerSocket(port, null, null));
    }

    /**
     * Create server on specified port and bind address.
     *
     * @param port the port to bind to
     * @param bindAddress the IP address to bind to (null = all interfaces, "127.0.0.1" = localhost only)
     * @throws Exception if server creation fails or address is invalid
     */
    public SingleThreadTcpRestServer(int port, String bindAddress) throws Exception {
        this(TcpRestServerSocketFactory.getServerSocket(port, bindAddress, null));
    }

    /**
     * Create SSL server on specified port binding to all interfaces.
     *
     * @param port the port to bind to
     * @param sslParams SSL parameters
     * @throws Exception if server creation fails
     */
    public SingleThreadTcpRestServer(int port, SSLParams sslParams) throws Exception {
        this(TcpRestServerSocketFactory.getServerSocket(port, null, sslParams));
    }

    /**
     * Create SSL server on specified port and bind address.
     *
     * @param port the port to bind to
     * @param bindAddress the IP address to bind to (null = all interfaces, "127.0.0.1" = localhost only)
     * @param sslParams SSL parameters
     * @throws Exception if server creation fails or address is invalid
     */
    public SingleThreadTcpRestServer(int port, String bindAddress, SSLParams sslParams) throws Exception {
        this(TcpRestServerSocketFactory.getServerSocket(port, bindAddress, sslParams));
    }

    /**
     * Create server with existing ServerSocket.
     *
     * @param socket the server socket to use
     */
    public SingleThreadTcpRestServer(ServerSocket socket) {
        this.serverSocket = socket;
        logger.info("ServerSocket initialized: " + this.serverSocket);
    }


    @Override
    public int getServerPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void up() {
        up(false);
    }

    @Override
    public void up(boolean setDaemon) {
        status = TcpRestServerStatus.RUNNING;
        initializeProtocolComponents();
        serverThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (status.equals(TcpRestServerStatus.RUNNING) && !Thread.currentThread().isInterrupted()) {
                        Socket socket = null;
                        PrintWriter writer = null;
                        try {
                            socket = serverSocket.accept();
                            logger.fine("Client accepted.");
                            writer = new PrintWriter(socket.getOutputStream());
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                 Scanner scanner = new Scanner(reader)) {
                                String request = scanner.nextLine();
                                String response = processRequest(request);
                                writer.println(response);
                                writer.flush();
                            }
                        } catch (ClassNotFoundException e) {
                            String message = "***SingleThreadTcpRestServer: requested class not found.";
                            logger.severe(message);
                            writer.println(message);
                        } catch (NoSuchMethodException e) {
                            String message = "***SingleThreadTcpRestServer: requested method not found.";
                            logger.severe(message);
                            writer.println(message);
                        } catch (InstantiationException e) {
                            String message = "***SingleThreadTcpRestServer: resource cannot be instantiated.";
                            logger.severe(message);
                            writer.println(message);
                        } catch (IllegalAccessException e) {
                            String message = "***SingleThreadTcpRestServer: cannot invoke context.";
                            logger.severe(message);
                            writer.println(message);
                        } catch (Exception e) {
                            if (e instanceof IOException) throw (IOException) e;
                            logger.severe(e.getMessage());
                            if (writer != null) writer.println(e.getMessage());
                        } finally {
                            if (writer != null) {
                                try { writer.close(); } catch (Exception ignored) { }
                            }
                            if (socket != null) {
                                try { socket.close(); } catch (IOException ignored) { }
                            }
                        }
                    }
                } catch (java.net.SocketException e) {
                    logger.fine("Server socket closed: " + e.getMessage());
                } catch (IOException e) {
                    logger.severe("IO error in server: " + e.getMessage());
                } finally {
                    logger.info("Server stopped.");
                }
            }
        };
        serverThread.setDaemon(setDaemon);
        serverThread.start();
    }

    @Override
    public void down() {
        status = TcpRestServerStatus.CLOSING;

        // Interrupt server thread
        if (serverThread != null) {
            serverThread.interrupt();
        }

        // Close server socket to unblock accept()
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.severe("Error closing server socket: " + e.getMessage());
            }
        }

        // Wait for thread termination (5 second timeout)
        if (serverThread != null) {
            try {
                serverThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
