package io.tcprest.invoker;

import io.tcprest.logger.Logger;
import io.tcprest.logger.LoggerFactory;
import io.tcprest.server.Context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class DefaultInvoker implements Invoker {
    private Logger logger = LoggerFactory.getDefaultLogger();

    public Object invoke(Context context) throws InstantiationException, IllegalAccessException {
        // get requested class
        Class clazz = context.getTargetClass();
        // get method to invoke
        Method method = context.getTargetMethod();
        try {
            return method.invoke(clazz.newInstance(), context.getParams());
        } catch (InvocationTargetException e) {
            logger.log("***DefaultInvoker: method invoking failed.");
            logger.log("Method: " + clazz.getCanonicalName() + "." + method.getName());
        }
        return null;
    }
}
