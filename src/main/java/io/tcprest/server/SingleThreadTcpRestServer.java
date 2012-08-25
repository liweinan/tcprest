package io.tcprest.server;

import io.tcprest.exception.MapperNotFoundException;
import io.tcprest.exception.ParseException;
import io.tcprest.extractor.DefaultExtractor;
import io.tcprest.extractor.Extractor;
import io.tcprest.invoker.DefaultInvoker;
import io.tcprest.invoker.Invoker;
import io.tcprest.logger.Logger;
import io.tcprest.logger.LoggerFactory;
import io.tcprest.mapper.Mapper;
import io.tcprest.mapper.MapperHelper;
import io.tcprest.ssl.SSLParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
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
public class SingleThreadTcpRestServer extends Thread implements TcpRestServer {

    private final Map<String, Mapper> mappers = new HashMap<String, Mapper>();

    private Logger logger = LoggerFactory.getDefaultLogger();

    private String status = TcpRestServerStatus.PASSIVE;

    private ServerSocket serverSocket;

    public final Map<String, Class> resourceClasses = new HashMap<String, Class>();

    public final Map<String, Object> singletonResources = new HashMap<String, Object>();

    public Extractor extractor = new DefaultExtractor(this);

    public Invoker invoker = new DefaultInvoker();

    public void addResource(Class resourceClass) {
        if (resourceClass == null) {
            return;
        }

        // Adding multiple instances of same class is meaningless. So every TcpRestServer implementation
        // should check and overwrite existing instances of same class and give out warning each time a
        // singleton resource is added.
        synchronized (resourceClasses) {
            if (resourceClasses.containsKey(resourceClass.getCanonicalName())) {
                logger.warn("Resource already exists for: " + resourceClass.getCanonicalName());
                return;
            }

            resourceClasses.put(resourceClass.getCanonicalName(), resourceClass);
        }
    }

    public void deleteResource(Class resourceClass) {
        synchronized (resourceClasses) {
            resourceClasses.remove(resourceClass.getCanonicalName());
        }
    }

    public void addSingletonResource(Object instance) {
        synchronized (singletonResources) {
            deleteSingletonResource(instance);
            singletonResources.put(instance.getClass().getCanonicalName(), instance);
        }
    }

    public void deleteSingletonResource(Object instance) {
        synchronized (singletonResources) {
            singletonResources.remove(instance.getClass().getCanonicalName());
        }
    }

    public Map<String, Class> getResourceClasses() {
        return new HashMap<String, Class>(resourceClasses);
    }

    public Map<String, Object> getSingletonResources() {
        return new HashMap<String, Object>(singletonResources);
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public SingleThreadTcpRestServer() throws Exception {
        this(TcpRestServerConfig.DEFAULT_PORT);
    }

    public SingleThreadTcpRestServer(int port) throws Exception {
        this(TcpRestServerFactory.getServerSocket(port, null));
    }

    public SingleThreadTcpRestServer(int port, SSLParam sslParam) throws Exception {
        this(TcpRestServerFactory.getServerSocket(port, sslParam));
    }

    public SingleThreadTcpRestServer(ServerSocket socket) {
        this.serverSocket = socket;

        // register default mappers
        for (String key : MapperHelper.DEFAULT_MAPPERS.keySet()) {
            mappers.put(key, MapperHelper.DEFAULT_MAPPERS.get(key));
        }

        logger.log("ServerSocket initialized: " + this.serverSocket);
    }

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
                logger.debug("request: " + request);
                // extract calling class and method from request
                Context context = extractor.extract(request);
                // invoke real method
                Object responseObject = invoker.invoke(context);
                logger.debug("***SingleThreadTcpRestServer - responseObject: " + responseObject);

                // get returned object and encode it to string response
                Mapper responseMapper = context.getConverter().getMapper(mappers, responseObject.getClass());

                writer.println(context.getConverter().encodeParam(responseMapper.objectToString(responseObject)));
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
            logger.debug("Server stopped.");
        }
    }

    public int getServerPort() {
        return serverSocket.getLocalPort();
    }

    public void up() {
        status = TcpRestServerStatus.RUNNING;
        this.start();
    }

    public void down() {
        status = TcpRestServerStatus.CLOSING;
    }

    public Map<String, Mapper> getMappers() {
        // We don't want user to modify the mappers by getMappers.
        // Use addMapper() to add mapper to server to ensure concurrency safety
        return new HashMap<String, Mapper>(this.mappers);
    }

    public void addMapper(String canonicalName, Mapper mapper) {
        synchronized (mappers) {
            mappers.put(canonicalName, mapper);
        }
    }
}
