package net.bluedash.tcprest.invoker;

import net.bluedash.tcprest.logger.Logger;
import net.bluedash.tcprest.logger.LoggerFactory;
import net.bluedash.tcprest.server.Context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author Weinan Li
 * Jul 29 2012
 */
public class DefaultInvoker implements Invoker {
    private Logger logger = LoggerFactory.getDefaultLogger();

    public Object invoke(Context context) throws InstantiationException, IllegalAccessException {
        // get requested class
        Class clazz = context.getTargetClass();
        // get method to invoke
        Method method = context.getTargetMethod();
        try {
            return (String) method.invoke(clazz.newInstance());
        } catch (InvocationTargetException e) {
            logger.log("***DefaultInvoker: method invoking failed.");
            logger.log("Method: " + clazz.getCanonicalName() + "." + method.getName());
        }
        return null;
    }
}
