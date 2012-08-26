package io.tcprest.server;

import io.tcprest.exception.MapperNotFoundException;
import io.tcprest.exception.ParseException;
import io.tcprest.ssl.SSLParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * SingleThreadTcpRestServer uses a single threaded Socket Server to serve the clients.
 * It's just for demonstration purpose.
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
// TODO support SSL
// TODO when server throws exception, put it into response object and return to user
// TODO check the resources when it's added, to see if it could be correctly mapped, if cannot find mapper and not serializable put warning.
public class SingleThreadTcpRestServer extends AbstractTcpRestServer {


    protected ServerSocket serverSocket;

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
        status = TcpRestServerStatus.RUNNING;
        new Thread() {
            public void run() {
                PrintWriter writer = null;
                try {
                    while (status.equals(TcpRestServerStatus.RUNNING)) {
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
                } catch (IOException e) {

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
                    try {
                        if (writer != null)
                            writer.flush();
                        serverSocket.close();
                    } catch (IOException e) {

                    }
                    logger.info("Server stopped.");
                }
            }
        }.start();
    }

    public void down() {
        status = TcpRestServerStatus.CLOSING;
    }

}
