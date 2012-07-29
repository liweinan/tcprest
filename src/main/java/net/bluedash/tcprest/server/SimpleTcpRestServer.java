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
 * SimpleTcpRestServer uses a single threaded Socket Server to serve the clients.
 * It's just for demonstration purpose.
 *
 * @author Weinan Li
 * Jul 29 2012
 */
public class SimpleTcpRestServer extends Thread implements TcpRestServer {

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


    public SimpleTcpRestServer() throws IOException {
        this.serverSocket = new ServerSocket(8001); // default port
        logger.log("ServerSocket initialized: " + this.serverSocket);
    }

    public SimpleTcpRestServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        logger.log("ServerSocket initialized: " + this.serverSocket);

    }

    public SimpleTcpRestServer(ServerSocket socket) {
        this.serverSocket = socket;
        logger.log("ServerSocket initialized: " + this.serverSocket);
    }

    public void run() {
        try {
            while (status == TcpRestServerStatus.RUNNING) {
                logger.log("Server started.");
                Socket socket = serverSocket.accept();
                logger.log("Client accepted.");
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(reader);

                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                while (true) {
                    try {
                        String request = scanner.nextLine();
                        logger.log("request: " +  request);
                        // extract calling class and method from request
                        Context context = extractor.extract(request);
                        // get response via invoker
                        String response = invoker.invoke(context);
                        writer.println(response);
                        writer.flush();
                    } catch (Exception e) {
                        writer.close();
                        socket.close();
                        logger.log(e.getClass().toString());
                        logger.log("Client disconnected.");
                        break;
                    }
                }
            }
        } catch (IOException e) {

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
