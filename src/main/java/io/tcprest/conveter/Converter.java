package io.tcprest.conveter;

import io.tcprest.exception.MapperNotFoundException;
import io.tcprest.mapper.Mapper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Converter is the the reverse operation of Extractor.
 * TcpRest client library uses it for transforming a method call into TcpRest communication protocol.
 * @author Weinan Li
 * @date 07 31 2012
 */
public interface Converter {
    /**
     * encode action
     * @param clazz Calling class
     * @param method Calling method
     * @param params parameters of calling method
     * @return converted request string
     */
    public String encode(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers) throws MapperNotFoundException;

    public Object[] decode(Method targetMethod, String paramToken, Map<String, Mapper> mappers) throws MapperNotFoundException;

    /**
     * Encapsulate the mapper processed message
     * @param message
     * @return
     */
    public String encodeParam(String message);

    public String decodeParam(String message);
}
