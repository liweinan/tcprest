package io.tcprest.client;

import io.tcprest.conveter.Converter;
import io.tcprest.conveter.DefaultConverter;
import io.tcprest.mapper.Mapper;
import io.tcprest.mapper.MapperHelper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;

/**
 * TcpRestClientProxy can generate a client from resource class/interface
 *
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class TcpRestClientProxy implements InvocationHandler {

    private TcpRestClient tcpRestClient;

    private Map<String, Mapper> mappers;

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, Map<String, Mapper> extraMappers) {
        //add default mappers
        mappers = MapperHelper.DEFAULT_MAPPERS;

        if (extraMappers != null) {
            mappers.putAll(extraMappers);
        }

        tcpRestClient = new DefaultTcpRestClient(deletgatedClassName, host, port);

    }

    public void setMappers(Map<String, Mapper> mappers) {
        this.mappers = mappers;
    }

    public Map<String, Mapper> getMappers() {
        return mappers;
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port) {
        this(deletgatedClassName, host, port, null);
    }

    public Object invoke(Object o, Method method, Object[] params) throws Throwable {
        String className = method.getDeclaringClass().getCanonicalName();
        if (!className.equals(tcpRestClient.getDeletgatedClassName())) {
            throw new IllegalAccessException("***TcpRestClientProxy - method cannot be invoked: " + method.getName());
        }
        Converter converter = new DefaultConverter();
        String request = converter.convert(method.getDeclaringClass(), method, params, mappers);
        String response = tcpRestClient.sendRequest(request);
        String[] respObj = converter.decode(response);
        Mapper mapper = mappers.get(respObj[0].getClass().getCanonicalName());
        if (mapper == null) {
            throw new IllegalAccessException("***TcpRestClientProxy - mapper cannot be found for response object: " + respObj[1].toString());
        }

        return mapper.stringToObject(respObj[1]);
    }

}
