package cn.huiwings.tcprest.invoker;

import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.protocol.NullObj;
import cn.huiwings.tcprest.server.Context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
@Deprecated
public class DefaultInvoker implements Invoker {
    private Logger logger = LoggerFactory.getDefaultLogger();

    public Object invoke(Context context) throws InstantiationException, IllegalAccessException {
        Object targetInstance;
        targetInstance = context.getTargetInstance(); // try to use singletonResource firstly
        if (targetInstance == null) { // search resource now
            // get requested class
            Class clazz = context.getTargetClass();
            targetInstance = clazz.newInstance();
        }
        logger.debug("***DefaultInvoker - targetInstance: " + targetInstance);
        // get method to invoke
        Method method = context.getTargetMethod();
        logger.debug("***DefaultInvoker - targetMethod: " + method.getName());
        try {
            Object respObj = method.invoke(targetInstance, context.getParams());
            if (respObj == null)
                respObj = new NullObj();
            logger.debug("***DefaultInvoker - respObj: " + respObj);
            return respObj;
        } catch (InvocationTargetException e) {
            logger.debug("***DefaultInvoker: method invoking failed.");
            logger.debug("Method: " + targetInstance.getClass().getCanonicalName() + "." + method.getName());
        }
        return new NullObj();
    }
}
