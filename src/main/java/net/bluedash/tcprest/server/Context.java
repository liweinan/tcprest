package net.bluedash.tcprest.server;

import java.lang.reflect.Method;
import java.util.List;

/**
 *
 * @author Weinan Li
 * Jul 29 2012
 */
public class Context {
    private Class targetClazz;
    private Method targetMethod;
    private List<Object> params;

    public Context() {
    }

    public Class getTargetClass() {
        return targetClazz;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public void setTargetClass(Class clazz) {
        this.targetClazz = clazz;
    }

    public void setTargetMethod(Method mtd) {
        this.targetMethod = mtd;
    }
}
