package io.tcprest.annotations;

import io.tcprest.logger.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author Weinan Li
 * @created_at 08 21 2012
 */
public class ParamHandler {

    public static int getTimeout(Method mtd) {
        for (Annotation atn : mtd.getAnnotations()) {
            if (atn.annotationType().equals(Timeout.class)) {
                LoggerFactory.getDefaultLogger().debug("***ParamHandler - Timeout: " + ((Timeout) atn).second());
                return ((Timeout) atn).second();
            }
        }
        return 0;
    }
}
