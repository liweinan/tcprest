package net.bluedash.tcprest.extractor;

import net.bluedash.tcprest.exception.ParseException;
import net.bluedash.tcprest.logger.Logger;
import net.bluedash.tcprest.logger.LoggerFactory;
import net.bluedash.tcprest.server.Context;
import net.bluedash.tcprest.server.SingleThreadTcpRestServer;
import net.bluedash.tcprest.server.TcpRestServer;

import java.lang.reflect.Method;
import java.util.List;

/**
 * The default request extractor. Now it just support string arguments.
 * <p/>
 * The following requests are valid:
 * <pre>
 * {@code
 * HelloWorldRestlet/sayHello
 * HelloWorldRestlet/sayHello()
 * HelloWorldRestlet/sayHelloTo(Jack)
 * HelloWorldRestlet/sayHelloTo(Jack!)
 * HelloWorldRestlet/sayHelloToPeople(you,me)
 * }
 * </pre>
 * <p/>
 * <p/>
 * In the future I want to make DefaultExtractor supports complex parameter types via mapper scheme.
 *
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class DefaultExtractor implements Extractor {

    private Logger logger = LoggerFactory.getDefaultLogger();

    private TcpRestServer tcpRestServer;

    public DefaultExtractor(SingleThreadTcpRestServer server) {
        this.tcpRestServer = server;
    }

    public Context extract(String request) throws ClassNotFoundException, NoSuchMethodException, ParseException {
        // class/method(arg1, arg2, ...)
        // get class and method from request


        // We do some sanity check firstly
        if (request.lastIndexOf(')') != request.length() - 1) {
            throw new ParseException("***DefaultExtractor: cannot parse request: " + request);
        }


        // search first '/'
        int classSeperator = request.indexOf('/');
        if (classSeperator < 1) {
            throw new NoSuchMethodException("***DefaultExtractor: Cannot find a method from request.");
        }

        // get class name
        String clazzName = request.substring(0, classSeperator);


        // search first '(' to extract method
        int methodSeperator = request.indexOf('(');
        // things like 'MyClass/()' is also not valid
        // so we need to ensure the format is at least 'MyClass/m()'
        // the methodSeperator - 1 is the 'm'
        // the classSeperator + 1 is '/'
        // So we check the method do exists
        if (methodSeperator - 1 < classSeperator + 1) {
            throw new NoSuchMethodException("***DefaultExtractor: Cannot find a valid method call from request.");
        }

        String methodName = request.substring(classSeperator + 1, methodSeperator);

        // get parameters
        // strip the quotes and extract all params
        String paramsToken = request.substring(methodSeperator + 1 , request.length() - 1);
        String params[];

        logger.log("***DefaultExtractor - paramsToken: " + paramsToken.trim());
        if (paramsToken.trim().length() < 1) {
            params = null;
        } else {
            params = paramsToken.trim().split(",");
        }

        // Now we fill context
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
            throw new ClassNotFoundException("***DefaultExtractor - Cannot find class: " + clazzName);

        // search method
        for (Method mtd : ctx.getTargetClass().getDeclaredMethods()) {
            logger.log("searching method: " + mtd.getName());
            if (mtd.getName().equals(methodName)) {
                ctx.setTargetMethod(mtd);
                break;
            }
        }

        if (ctx.getTargetMethod() == null)
            throw new NoSuchMethodException("***DefaultExtractor - Cannot find method: " + methodName);

        // fill arguments
        ctx.setParams(params);

        return ctx;
    }

}
