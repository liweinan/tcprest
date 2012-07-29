package net.bluedash.tcprest.extractor;

import net.bluedash.tcprest.server.Context;
import net.bluedash.tcprest.server.SimpleTcpRestServer;
import net.bluedash.tcprest.server.TcpRestServer;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: weli
 * Date: 7/29/12
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultExtractor implements Extractor {

    private TcpRestServer tcpRestServer;

    public DefaultExtractor(SimpleTcpRestServer server) {
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
            System.out.println("searching class: " + clazz.getCanonicalName());
            if (clazz.getCanonicalName().equals(clazzName)) {
                ctx.setTargetClass(clazz);
                break;
            }
        }

        if (ctx.getTargetClass() == null)
            throw new ClassNotFoundException();

        // search method
        for (Method mtd : ctx.getTargetClass().getDeclaredMethods()) {
            System.out.println("searching method: " + mtd.getName());
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
