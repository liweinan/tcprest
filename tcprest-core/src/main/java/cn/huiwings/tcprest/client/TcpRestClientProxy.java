package cn.huiwings.tcprest.client;

import cn.huiwings.tcprest.annotations.TimeoutAnnotationHandler;
import cn.huiwings.tcprest.conveter.Converter;
import cn.huiwings.tcprest.conveter.DefaultConverter;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.MapperHelper;
import cn.huiwings.tcprest.protocol.NullObj;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.ssl.SSLParam;

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
