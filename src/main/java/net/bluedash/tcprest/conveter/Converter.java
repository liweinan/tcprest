package net.bluedash.tcprest.conveter;

import net.bluedash.tcprest.exception.MapperNotFoundException;
import net.bluedash.tcprest.mapper.Mapper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Converter is a low-level tool that TcpRest client library used for transforming a method call into TcpRest communication protocol.
 * @author Weinan Li
 * @date 07 31 2012
 */
public interface Converter {
    /**
     * convert action
     * @param clazz Calling class
     * @param method Calling method
     * @param params parameters of calling method
     * @return converted request string
     */
    public String convert(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers) throws MapperNotFoundException;

    /**
     * Encapsulate the mapper processed message
     * @param message
     * @return
     */
    public String encode(String message, Class messageType);

    public String[] decode(String message);
}
