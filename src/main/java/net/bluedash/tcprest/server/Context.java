package net.bluedash.tcprest.server;

import java.lang.reflect.Method;

/**
 * Created by IntelliJ IDEA.
 * User: weli
 * Date: 7/29/12
 * Time: 3:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class Context {
    private Method targetMethod;
    private Class targetClazz;

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
