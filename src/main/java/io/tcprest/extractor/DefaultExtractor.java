package io.tcprest.extractor;

import io.tcprest.exception.MapperNotFoundException;
import io.tcprest.exception.ParseException;
import io.tcprest.logger.Logger;
import io.tcprest.logger.LoggerFactory;
import io.tcprest.mapper.Mapper;
import io.tcprest.protocol.TcpRestProtocol;
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

        // First we combine resources and singletonResources to search
        List<Class> classesToSearch = tcpRestServer.getResourceClasses();
        for (Object instance : tcpRestServer.getSingletonResources()) {
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

        // get parameters
        // strip the quotes and extract all params
        String paramsToken = request.substring(methodSeperator + 1, request.length() - 1);

        Object params[] = null;
        logger.log("***DefaultExtractor - paramsToken: " + paramsToken.trim());
        if (paramsToken.trim().length() < 1) {
            params = null;
        } else {
            // todo merge this part of code with Converter
            // todo put it into a new class called ProtocolStack
            // unprocessed rawParams
            // such as:  {{Jack!}}java.lang.String
            // We need to convert it to proper types
            String rawParams[] = paramsToken.trim().split(TcpRestProtocol.PATH_SEPERATOR);
            List<Object> paramsHolder = new ArrayList<Object>();
            for (String rawParam : rawParams) {
                // pick the value of param
                String val = rawParam.substring(rawParam.indexOf("{{") + 2, rawParam.indexOf("}}"));
                logger.log("***DefaultExtractor - param value: " + val);
                String classType = rawParam.substring(rawParam.indexOf("}}") + 2, rawParam.length());
                logger.log("***DefaultExtractor - param type: " + classType);
                Mapper mapper = tcpRestServer.getMappers().get(classType.trim());
                if (mapper == null) {
                    throw new MapperNotFoundException("***DefaultExtractor - cannot find mapper for: " + classType);
                }

                Object param = mapper.stringToObject(val);
                paramsHolder.add(param);
            }
            params = paramsHolder.toArray();
        }

        // Now we fill context
        Context ctx = new Context();

        // We'll search for relative class now. We first search in singletonResources,
        // then we search in resources. So if two same class appear both in resources and
        // singletonResources(one is class and the other is instance of the class), the latter will hit first.

        // search clazz from tcpRestServer singletonResources firstly
        List<Object> instances = tcpRestServer.getSingletonResources();

        for (Object instance : instances) {
            logger.log("***DefaultExtractor - searching singleton resources: " + instance.getClass().getCanonicalName());
            if (instance.getClass().getCanonicalName().equals(clazzName)) {
                ctx.setTargetInstance(instance);
                break;
            }
        }

        List<Class> classes = tcpRestServer.getResourceClasses();
        for (Class clazz : classes) {
            logger.log("***DefaultExtractor - searching class: " + clazz.getCanonicalName());
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
