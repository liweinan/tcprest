package net.bluedash.tcprest.extractor;

import net.bluedash.tcprest.logger.Logger;
import net.bluedash.tcprest.logger.LoggerFactory;
import net.bluedash.tcprest.server.Context;
import net.bluedash.tcprest.server.SingleThreadTcpRestServer;
import net.bluedash.tcprest.server.TcpRestServer;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Weinan Li
 */
public class DefaultExtractor implements Extractor {

    private Logger logger = LoggerFactory.getDefaultLogger();

    private TcpRestServer tcpRestServer;

    public DefaultExtractor(SingleThreadTcpRestServer server) {
        this.tcpRestServer = server;
    }

    public Context extract(String request) throws ClassNotFoundException, NoSuchMethodException {
        // get class and method from request
        String[] segments = request.split("/");
        String clazzName = segments[0];
        String methodName = segments[1];

        Context ctx = new Context();

        // search clazz from tcpRestServer instance
        List<Class> classes = tcpRestServer.getResourceClasses();
        for (Class clazz : classes) {
            logger.log("searching class: " + clazz.getCanonicalName());
            if (clazz.getCanonicalName().equals(clazzName)) {
                ctx.setTargetClass(clazz);
                break;
            }
        }

        if (ctx.getTargetClass() == null)
            throw new ClassNotFoundException();

        // search method
        for (Method mtd : ctx.getTargetClass().getDeclaredMethods()) {
            logger.log("searching method: " + mtd.getName());
            if (mtd.getName().equals(methodName)) {
                ctx.setTargetMethod(mtd);
                break;
            }
        }

        if (ctx.getTargetMethod() == null)
            throw new NoSuchMethodException();

        return ctx;
    }

}
