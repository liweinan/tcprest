package io.tcprest.server;

import io.tcprest.conveter.Converter;
import io.tcprest.conveter.DefaultConverter;
import io.tcprest.exception.MapperNotFoundException;
import io.tcprest.exception.ParseException;
import io.tcprest.extractor.DefaultExtractor;
import io.tcprest.extractor.Extractor;
import io.tcprest.invoker.DefaultInvoker;
import io.tcprest.invoker.Invoker;
import io.tcprest.logger.Logger;
import io.tcprest.logger.LoggerFactory;
import io.tcprest.mapper.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * SingleThreadTcpRestServer uses a single threaded Socket Server to serve the clients.
 * It's just for demonstration purpose.
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class SingleThreadTcpRestServer extends Thread implements TcpRestServer {

    private Map<String, Mapper> mappers = new HashMap<String, Mapper>();

    private Logger logger = LoggerFactory.getDefaultLogger();

    private String status = TcpRestServerStatus.PASSIVE;

    private ServerSocket serverSocket;

    public List<Class> resourceClasses = new ArrayList<Class>();

    public Extractor extractor = new DefaultExtractor(this);

    public Invoker invoker = new DefaultInvoker();


    public void addResource(Class resourceClass) {
        resourceClasses.add(resourceClass);

    }

    // TODO not thread safe now, if some clients are calling this resource it will cause problem.
    public void deleteResource(Class resourceClass) {
        resourceClasses.remove(resourceClass);
    }

    public List<Class> getResourceClasses() {
        return resourceClasses;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public SingleThreadTcpRestServer() throws IOException {
        this(8001);
    }

    public SingleThreadTcpRestServer(int port) throws IOException {
        this(new ServerSocket(port));
    }

    public SingleThreadTcpRestServer(ServerSocket socket) {
        this.serverSocket = socket;

        // register default mappers
        mappers = MapperHelper.DEFAULT_MAPPERS;

        logger.log("ServerSocket initialized: " + this.serverSocket);
    }

    // todo throw staandard error message.
    // todo desgin error message
    public void run() {
        PrintWriter writer = null;
        try {
            while (status.equals(TcpRestServerStatus.RUNNING)) {
                logger.log("Server started.");
                Socket socket = serverSocket.accept();
                logger.log("Client accepted.");
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(reader);

                writer = new PrintWriter(socket.getOutputStream());
                while (scanner.hasNext()) {
                    String request = scanner.nextLine();
                    logger.log("request: " + request);
                    // extract calling class and method from request
                    Context context = extractor.extract(request);
                    // invoke real method
                    Object responseObject = invoker.invoke(context);

                    // get returned object and convert it to string response
                    Mapper responseMapper = mappers.get(responseObject.getClass().getCanonicalName());
                    if (responseMapper == null) {
                        throw new MapperNotFoundException("***SingleThreadTcpRestServer - mapper not found for response object: " +
                                responseObject.toString());
                    }

                    Converter converter = new DefaultConverter();
                    writer.println(converter.encode(responseMapper.objectToString(responseObject), responseObject.getClass()));
                    writer.flush();
                }
            }
        } catch (IOException e) {

        } catch (ClassNotFoundException e) {
            String message = "***SingleThreadTcpRestServer: requested class not found.";
            logger.log(message);
            if (writer != null)
                writer.println(message);
        } catch (NoSuchMethodException e) {
            String message = "***SingleThreadTcpRestServer: requested method not found.";
            logger.log(message);
            if (writer != null)
                writer.println(message);
        } catch (InstantiationException e) {
            String message = "***SingleThreadTcpRestServer: requested method not found.";
            logger.log(message);
            if (writer != null)
                writer.println(message);
        } catch (IllegalAccessException e) {
            String message = "***SingleThreadTcpRestServer: cannot invoke context.";
            logger.log(message);
            if (writer != null)
                writer.println(message);
        } catch (ParseException e) {
            logger.log(e.getMessage());
            if (writer != null)
                writer.println(e.getMessage());
        } catch (MapperNotFoundException e) {
            logger.log(e.getMessage());
            if (writer != null)
                writer.println(e.getMessage());
        } finally {
            try {
                if (writer != null)
                    writer.flush();
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

    // TODO return a cloned copy
    public Map<String, Mapper> getMappers() {
        return mappers;
    }

    public void setMappers(Map<String, Mapper> mappers) {
        this.mappers = mappers;
    }

    // TODO not thread safe now
    public void addMapper(String canonicalName, Mapper mapper) {
        mappers.put(canonicalName, mapper);
    }
}
