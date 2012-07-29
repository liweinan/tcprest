package net.bluedash.tcprest.server;

import net.bluedash.tcprest.extractor.DefaultExtractor;
import net.bluedash.tcprest.extractor.Extractor;
import net.bluedash.tcprest.invoker.DefaultInvoker;
import net.bluedash.tcprest.invoker.Invoker;
import net.bluedash.tcprest.logger.Logger;
import net.bluedash.tcprest.logger.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * SingleThreadTcpRestServer uses a single threaded Socket Server to serve the clients.
 * It's just for demonstration purpose.
 *
 * @author Weinan Li
 *         Jul 29 2012
 */
public class SingleThreadTcpRestServer extends Thread implements TcpRestServer {

    private Logger logger = LoggerFactory.getDefaultLogger();

    private String status = TcpRestServerStatus.PASSIVE;

    private ServerSocket serverSocket;

    public List<Class> resourceClasses = new ArrayList<Class>();

    public Extractor extractor = new DefaultExtractor(this);

    public Invoker invoker = new DefaultInvoker();


    public void addResource(Class resourceClass) {
        resourceClasses.add(resourceClass);

    }

    public List<Class> getResourceClasses() {
        return resourceClasses;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }


    public SingleThreadTcpRestServer() throws IOException {
        this.serverSocket = new ServerSocket(8001); // default port
        logger.log("ServerSocket initialized: " + this.serverSocket);
    }

    public SingleThreadTcpRestServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        logger.log("ServerSocket initialized: " + this.serverSocket);

    }

    public SingleThreadTcpRestServer(ServerSocket socket) {
        this.serverSocket = socket;
        logger.log("ServerSocket initialized: " + this.serverSocket);
    }

    public void run() {
        try {
            while (status.equals(TcpRestServerStatus.RUNNING)) {
                logger.log("Server started.");
                Socket socket = serverSocket.accept();
                logger.log("Client accepted.");
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(reader);

                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                while (scanner.hasNext()) {
                    String request = scanner.nextLine();
                    logger.log("request: " + request);
                    // extract calling class and method from request
                    Context context = extractor.extract(request);
                    // invoke real method
                    String response = (String) invoker.invoke(context);
                    writer.println(response);
                    writer.flush();
                }
            }
        } catch (IOException e) {

        } catch (ClassNotFoundException e) {
            logger.log("***SingleThreadTcpRestServer: requested class not found.");
        } catch (NoSuchMethodException e) {
            logger.log("***SingleThreadTcpRestServer: requested method not found.");
        } catch (InstantiationException e) {
            logger.log("***SingleThreadTcpRestServer: cannot invoke context.");
        } catch (IllegalAccessException e) {
            logger.log("***SingleThreadTcpRestServer: cannot invoke context.");
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {

            }
            logger.log("Server stopped.");
        }
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void up() {
        status = TcpRestServerStatus.RUNNING;
        this.start();
    }

    public void down() {
        status = TcpRestServerStatus.CLOSING;
    }
}
