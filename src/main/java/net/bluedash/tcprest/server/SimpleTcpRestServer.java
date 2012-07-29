package net.bluedash.tcprest.server;

import net.bluedash.tcprest.extractor.DefaultExtractor;
import net.bluedash.tcprest.extractor.Extractor;
import net.bluedash.tcprest.invoker.DefaultInvoker;
import net.bluedash.tcprest.invoker.Invoker;

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
 * SimpleTcpRestServer is a single thread Socket Server.
 *
 * @author Weinan Li
 * Jul 29 2012
 */
public class SimpleTcpRestServer extends Thread implements TcpRestServer {

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


    public SimpleTcpRestServer() throws IOException {
        this.serverSocket = new ServerSocket(8001); // default port
        System.out.println("ServerSocket initialized: " + this.serverSocket);
    }

    public SimpleTcpRestServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("ServerSocket initialized: " + this.serverSocket);

    }

    public SimpleTcpRestServer(ServerSocket socket) {
        this.serverSocket = socket;
        System.out.println("ServerSocket initialized: " + this.serverSocket);
    }

    public void run() {
        try {
            while (status == TcpRestServerStatus.RUNNING) {
                System.out.println("Server started.");
                Socket socket = serverSocket.accept();
                System.out.println("Client accepted.");
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(reader);

                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                while (true) {
                    try {
                        String request = scanner.nextLine();
                        System.out.printf("request: %s\n", request);
                        // extract calling class and method from request
                        Context context = extractor.extract(request);
                        // get response via invoker
                        String response = invoker.invoke(context);
                        writer.println(response);
                        writer.flush();
                    } catch (Exception e) {
                        writer.close();
                        socket.close();
                        System.out.println(e.getClass().toString());
                        System.out.println("Client disconnected.");
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
            System.out.println("Server stopped.");
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
