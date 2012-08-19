package io.tcprest.extractor;

import io.tcprest.conveter.Converter;
import io.tcprest.conveter.DefaultConverter;
import io.tcprest.exception.MapperNotFoundException;
import io.tcprest.exception.ParseException;
import io.tcprest.logger.Logger;
import io.tcprest.logger.LoggerFactory;
import io.tcprest.server.Context;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.server.TcpRestServer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The default request extractor. Now it just support string arguments.
 * <p/>
 * The following requests are valid:
 * <pre>
 * {@code
 * HelloWorldRestlet/sayHello
 * HelloWorldResource/sayHello()
 * HelloWorldResource/sayHelloTo(Jack)
 * HelloWorldResource/sayHelloTo(Jack!)
 * HelloWorldResource/sayHelloToPeople(you,me)
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

    private final Converter converter = new DefaultConverter();

    public DefaultExtractor(SingleThreadTcpRestServer server) {
        this.tcpRestServer = server;
    }

    public Context extract(String request) throws ClassNotFoundException, NoSuchMethodException, ParseException, MapperNotFoundException {
        // class/method(arg1, arg2, ...)
        // get class and method from request

        // We do some sanity check firstly
        if (request == null || (request.lastIndexOf(')') != request.length() - 1)) {
            throw new ParseException("***DefaultExtractor: cannot parse request: " + request);
        }

        // search first '/'
        int classSeperator = request.indexOf('/');
        if (classSeperator < 1) {
            throw new NoSuchMethodException("***DefaultExtractor: Cannot find a method from request.");
        }

        // get class name
        String clazzName = request.substring(0, classSeperator);

        // we need to check whether the server has the relative resource or not
        // If client is using interface, we'll check whether the server has implemented resources
        // or not.
        List<Class> classesToSearch = new ArrayList<Class>();

        for (Class clazz : tcpRestServer.getResourceClasses().values()) {
            classesToSearch.add(clazz);
        }

        for (Object instance : tcpRestServer.getSingletonResources().values()) {
            classesToSearch.add(instance.getClass());
        }

        // The search logic
        for (Class clazz : classesToSearch) {
            if (clazzName.equals(clazz.getCanonicalName())) {
                break; // we've found it.
            } else { // otherwise we check if it's an interface that has an implmented class resource in server
                for (Class ifc : clazz.getInterfaces()) {
                    if (ifc.getCanonicalName().equals(clazzName)) {
                        logger.log("***DefaultExtractor - found implemented class: " + clazz.getCanonicalName());
                        clazzName = clazz.getCanonicalName();
                        break;
                    }
                }
            }
        }

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

        // Now we start to fill in context
        Context ctx = new Context();

        // We'll search for resources now. We first search in singleton resources,
        // then we search in resource classes.
        // If two same class appear both in singleton and class resources, we'll
        // use the singleton one.
        if (tcpRestServer.getSingletonResources().containsKey(clazzName)) {
            ctx.setTargetInstance(tcpRestServer.getSingletonResources().get(clazzName));
            ctx.setTargetClass(tcpRestServer.getSingletonResources().get(clazzName).getClass());
        } else if (tcpRestServer.getResourceClasses().containsKey(clazzName)) {
            ctx.setTargetClass(tcpRestServer.getResourceClasses().get(clazzName));
        }

        if (ctx.getTargetClass() == null)
            throw new ClassNotFoundException("***DefaultExtractor - Can't find resource for: " + clazzName);

        // search method
        for (Method mtd : ctx.getTargetClass().getDeclaredMethods()) {
            logger.debug("searching method: " + mtd.getName());
            if (mtd.getName().equals(methodName)) {
                ctx.setTargetMethod(mtd);
                break;
            }
        }

        if (ctx.getTargetMethod() == null)
            throw new NoSuchMethodException("***DefaultExtractor - Can't find method: " + methodName);

        // get parameters
        // strip the quotes and extract all params
        String paramsToken = request.substring(methodSeperator + 1, request.length() - 1);

        Object params[] = converter.decode(ctx.getTargetMethod(), paramsToken, tcpRestServer.getMappers());

        // fill arguments
        ctx.setParams(params);

        return ctx;
    }

}
