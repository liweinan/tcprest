package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.exception.ParseException;
import cn.huiwings.tcprest.ssl.SSLParam;

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
 * <p><b>SSL Support:</b> This server supports SSL/TLS via {@link cn.huiwings.tcprest.ssl.SSLParam}.</p>
 *
 * <p><b>Use cases:</b></p>
 * <ul>
 *   <li>Development and testing</li>
 *   <li>Low-traffic applications</li>
 *   <li>Applications requiring SSL without external dependencies</li>
 * </ul>
 *
 * <p><b>Performance:</b> Single-threaded, handles one request at a time.
 * For high-concurrency scenarios, use {@link NioTcpRestServer} (without SSL)
 * or {@code NettyTcpRestServer} (with SSL).</p>
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
// TODO when server throws exception, put it into response object and return to user
// TODO check the resources when it's added, to see if it could be correctly mapped, if cannot find mapper and not serializable put warning.
public class SingleThreadTcpRestServer extends AbstractTcpRestServer {


    protected ServerSocket serverSocket;
    private volatile Thread serverThread;

    public SingleThreadTcpRestServer() throws Exception {
        this(TcpRestServerConfig.DEFAULT_PORT);
    }

    public SingleThreadTcpRestServer(int port) throws Exception {
        this(TcpRestServerSocketFactory.getServerSocket(port, null));
    }

    public SingleThreadTcpRestServer(int port, SSLParam sslParam) throws Exception {
        this(TcpRestServerSocketFactory.getServerSocket(port, sslParam));
    }

    public SingleThreadTcpRestServer(ServerSocket socket) {
        this.serverSocket = socket;
        logger.info("ServerSocket initialized: " + this.serverSocket);
    }


    public int getServerPort() {
        return serverSocket.getLocalPort();
    }

    public void up() {
        up(false);
    }

    public void up(boolean setDaemon) {
        status = TcpRestServerStatus.RUNNING;
        serverThread = new Thread() {
            public void run() {
                PrintWriter writer = null;
                try {
                    while (status.equals(TcpRestServerStatus.RUNNING) && !Thread.currentThread().isInterrupted()) {
                        Socket socket = serverSocket.accept();
                        logger.debug("Client accepted.");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        Scanner scanner = new Scanner(reader);

                        writer = new PrintWriter(socket.getOutputStream());

                        String request = scanner.nextLine();

                        String response = processRequest(request);

                        writer.println(response);
                        writer.flush();

                    }
                } catch (java.net.SocketException e) {
                    // Expected during shutdown when serverSocket.close() is called
                    logger.debug("Server socket closed: " + e.getMessage());
                } catch (IOException e) {
                    logger.error("IO error in server: " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    String message = "***SingleThreadTcpRestServer: requested class not found.";
                    logger.error(message);
                    if (writer != null)
                        writer.println(message);
                } catch (NoSuchMethodException e) {
                    String message = "***SingleThreadTcpRestServer: requested method not found.";
                    logger.error(message);
                    if (writer != null)
                        writer.println(message);
                } catch (InstantiationException e) {
                    String message = "***SingleThreadTcpRestServer: resource cannot be instantiated.";
                    logger.error(message);
                    if (writer != null)
                        writer.println(message);
                } catch (IllegalAccessException e) {
                    String message = "***SingleThreadTcpRestServer: cannot invoke context.";
                    logger.error(message);
                    if (writer != null)
                        writer.println(message);
                } catch (ParseException e) {
                    logger.error(e.getMessage());
                    if (writer != null)
                        writer.println(e.getMessage());
                } catch (MapperNotFoundException e) {
                    logger.error(e.getMessage());
                    if (writer != null)
                        writer.println(e.getMessage());
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    if (writer != null)
                        writer.println(e.getMessage());
                } finally {
                    // Only close client resources, not server socket
                    if (writer != null) {
                        writer.close();
                    }
                    logger.info("Server stopped.");
                }
            }
        };
        serverThread.setDaemon(setDaemon);
        serverThread.start();
    }

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
                logger.error("Error closing server socket: " + e.getMessage());
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
