package net.bluedash.tcprest.extractor;

import net.bluedash.tcprest.logger.Logger;
import net.bluedash.tcprest.logger.LoggerFactory;
import net.bluedash.tcprest.server.Context;
import net.bluedash.tcprest.server.SingleThreadTcpRestServer;
import net.bluedash.tcprest.server.TcpRestServer;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
        // class/method(arg1, arg2, ...)
        // get class and method from request
        String[] segments = request.split("/");
        String clazzName = segments[0];

        // method | arg1, arg2, ...)
        String[] methodAndArgs = segments[1].split("\\(");
        String methodName = methodAndArgs[0];

        // extract args
        // remove the ')' at the end
        // arg1, arg2, ...
        String[] args;
        if (methodAndArgs.length > 1) {
            String argToken = methodAndArgs[1].substring(0, methodAndArgs[1].length() - 1);
            args = argToken.split(",");
        } else {
            args = null;
        }


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

        // process arguments
        if (args != null) {
            List<Object> params = new ArrayList<Object>();
            for (String arg : args) {
                params.add(arg.trim());
            }
            ctx.setParams(params.toArray());
        }

        return ctx;
    }

}
