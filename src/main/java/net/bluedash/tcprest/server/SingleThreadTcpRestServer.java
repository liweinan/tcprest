package net.bluedash.tcprest.server;

import net.bluedash.tcprest.conveter.Converter;
import net.bluedash.tcprest.conveter.DefaultConverter;
import net.bluedash.tcprest.exception.MapperNotFoundException;
import net.bluedash.tcprest.exception.ParseException;
import net.bluedash.tcprest.extractor.DefaultExtractor;
import net.bluedash.tcprest.extractor.Extractor;
import net.bluedash.tcprest.invoker.DefaultInvoker;
import net.bluedash.tcprest.invoker.Invoker;
import net.bluedash.tcprest.logger.Logger;
import net.bluedash.tcprest.logger.LoggerFactory;
import net.bluedash.tcprest.mapper.BooleanMapper;
import net.bluedash.tcprest.mapper.IntegerMapper;
import net.bluedash.tcprest.mapper.Mapper;
import net.bluedash.tcprest.mapper.StringMapper;

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
        mappers.put(String.class.getCanonicalName(), new StringMapper());
        mappers.put(Integer.class.getCanonicalName(), new IntegerMapper());
        mappers.put(Boolean.class.getCanonicalName(), new BooleanMapper());

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
                    Object responseObject = invoker.invoke(context);

                    // get returned object and convert it to string response
                    Mapper responseMapper = mappers.get(responseObject.getClass().getCanonicalName());
                    if (responseMapper == null) {
                        throw new MapperNotFoundException("***SingleThreadTcpRestServer - mapper not found for response object: " + responseObject.toString());
                    }

                    Converter converter = new DefaultConverter();
                    writer.println(converter.encode(responseMapper.objectToString(responseObject), responseObject.getClass()));
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
        } catch (ParseException e) {
            logger.log(e.getMessage());
        } catch (MapperNotFoundException e) {
            logger.log(e.getMessage());
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

    public Map<String, Mapper> getMappers() {
        return mappers;
    }

    public void setMappers(Map<String, Mapper> mappers) {
        this.mappers = mappers;
    }
}
