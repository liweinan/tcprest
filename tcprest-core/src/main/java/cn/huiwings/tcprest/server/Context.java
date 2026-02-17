package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.conveter.Converter;

import java.lang.reflect.Method;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class Context {
    private Class targetClazz;
    private Converter converter;

    public Converter getConverter() {
        return converter;
    }

    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    private Object targetInstance;
    private Method targetMethod;
    private Object[] params;
    private Object[] paramTypes;

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

    public Object[] getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(Object[] paramTypes) {
        this.paramTypes = paramTypes;
    }

    public Object getTargetInstance() {
        return targetInstance;
    }

    public void setTargetInstance(Object targetInstance) {
        this.targetInstance = targetInstance;
    }


}
