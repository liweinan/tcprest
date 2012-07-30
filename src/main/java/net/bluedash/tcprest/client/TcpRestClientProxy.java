package net.bluedash.tcprest.client;

import net.bluedash.tcprest.conveter.Converter;
import net.bluedash.tcprest.conveter.DefaultConverter;
import net.bluedash.tcprest.mapper.Mapper;
import net.bluedash.tcprest.mapper.StringMapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * TcpRestClientProxy can generate a client from resource class/interface
 *
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class TcpRestClientProxy implements InvocationHandler {

    private TcpRestClient tcpRestClient;

    private Map<String, Mapper> mappers = new HashMap<String, Mapper>();

    public void setMappers(Map<String, Mapper> mappers) {
        this.mappers = mappers;
    }

    public Map<String, Mapper> getMappers() {
        return mappers;
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port) {
        //add default mappers
        mappers.put(String.class.getCanonicalName(), new StringMapper());

        tcpRestClient = new DefaultTcpRestClient(deletgatedClassName, host, port);
    }

    public Object invoke(Object o, Method method, Object[] params) throws Throwable {
        String className = method.getDeclaringClass().getCanonicalName();
        if (!className.equals(tcpRestClient.getDeletgatedClassName())) {
            throw new IllegalAccessException("***TcpRestClientProxy - method cannot be invoked: " + method.getName());
        }
        Converter converter = new DefaultConverter();
        String request = converter.convert(method.getDeclaringClass(), method, params);
        String response = tcpRestClient.sendRequest(request);
        String[] respObj = converter.decode(response);
        Mapper mapper = mappers.get(respObj[0].getClass().getCanonicalName());
        if (mapper == null) {
            throw new IllegalAccessException("***TcpRestClientProxy - mapper cannot be found for response object: " + respObj[1].toString());
        }

        return mapper.stringToObject(respObj[1]);
    }

}
