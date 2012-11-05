package io.tcprest.client;

import io.tcprest.annotations.TimeoutAnnotationHandler;
import io.tcprest.conveter.Converter;
import io.tcprest.conveter.DefaultConverter;
import io.tcprest.logger.Logger;
import io.tcprest.logger.LoggerFactory;
import io.tcprest.mapper.Mapper;
import io.tcprest.mapper.MapperHelper;
import io.tcprest.protocol.NullObj;
import io.tcprest.protocol.TcpRestProtocol;
import io.tcprest.ssl.SSLParam;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * TcpRestClientProxy can generate a client from resource class/interface
 *
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class TcpRestClientProxy implements InvocationHandler {

    private Logger logger = LoggerFactory.getDefaultLogger();

    private TcpRestClient tcpRestClient;

    private Map<String, Mapper> mappers;

    private Converter converter = new DefaultConverter();

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, Map<String, Mapper> extraMappers, SSLParam sslParam) {
        mappers = MapperHelper.DEFAULT_MAPPERS;

        if (extraMappers != null) {
            mappers.putAll(extraMappers);
        }

        tcpRestClient = new DefaultTcpRestClient(sslParam, deletgatedClassName, host, port);
    }

    public void setMappers(Map<String, Mapper> mappers) {
        this.mappers = mappers;
    }

    public Map<String, Mapper> getMappers() {
        return mappers;
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port) {
        this(deletgatedClassName, host, port, null, null);
    }

    public TcpRestClientProxy(String deletgatedClassName, String host, int port, SSLParam sslParam) {
        this(deletgatedClassName, host, port, null, sslParam);
    }

    public Object invoke(Object o, Method method, Object[] params) throws Throwable {
        String className = method.getDeclaringClass().getCanonicalName();
        if (!className.equals(tcpRestClient.getDeletgatedClassName())) {
            throw new IllegalAccessException("***TcpRestClientProxy - method cannot be invoked: " + method.getName());
        }

        String request = converter.encode(method.getDeclaringClass(), method, params, mappers);

        String response = tcpRestClient.sendRequest(request, TimeoutAnnotationHandler.getTimeout(method));
        logger.debug("response: " + response);
        String respStr = converter.decodeParam(response);

        String mapperKey = method.getReturnType().getCanonicalName();
        if (respStr.equals(TcpRestProtocol.NULL))
            mapperKey = NullObj.class.getCanonicalName();

        logger.debug("***TcpRestClientProxy - response: " + respStr);

        Mapper mapper = converter.getMapper(mappers, mapperKey);

        logger.debug("***TcpRestClientProxy - mapper: " + mapper);

        if (mapper == null) {
            throw new IllegalAccessException("***TcpRestClientProxy - mapper cannot be found for response object: " + respStr.toString());
        }

        return mapper.stringToObject(respStr);
    }

}
