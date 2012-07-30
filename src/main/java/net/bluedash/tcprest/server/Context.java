package net.bluedash.tcprest.server;

import java.lang.reflect.Method;

/**
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class Context {
    private Class targetClazz;
    private Method targetMethod;
    private Object[] params;

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

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }
}
