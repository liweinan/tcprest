package io.tcprest.conveter;

import io.tcprest.exception.MapperNotFoundException;
import io.tcprest.logger.Logger;
import io.tcprest.logger.SystemOutLogger;
import io.tcprest.mapper.Mapper;
import io.tcprest.protocol.DefaultTcpRestProtocol;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * TcpRest Protocol:
 * Class.method(arg1, arg2) should transform to: "Class/method({{arg1}}arg1ClassName,{{arg2}}arg2ClassName)"
 * <p/>
 * For example:
 * HelloWorldResource.sayHelloFromTo("Jack", "Lucy") should transform to:
 * "HelloWorldResource/sayHelloFromTo({{Jack}}java.lang.String,{{Lucy}}java.lang.String)"
 *
 * @author Weinan Li
 * @date 07 31 2012
 */
public class DefaultConverter implements Converter {
    Logger logger = new SystemOutLogger();

    public String convert(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers) throws MapperNotFoundException {
        StringBuilder paramTokenBuffer = new StringBuilder();
        if (params != null) {
            for (Object param : params) {
                logger.log("***DefaultConverter: " + param.getClass());
                Mapper mapper = mappers.get(param.getClass().getCanonicalName());
                if (mapper == null) {
                    throw new MapperNotFoundException("***DefaultConverter - Cannot find mapper for: " + param.getClass().getCanonicalName());
                }

                paramTokenBuffer.append("{{").append(mapper.objectToString(param))
                        .append("}}").append(param.getClass().getCanonicalName()).append(DefaultTcpRestProtocol.PATH_SEPERATOR);
            }

            return clazz.getCanonicalName() + "/" + method.getName() + "(" + paramTokenBuffer.
                    substring(0, paramTokenBuffer.length() - DefaultTcpRestProtocol.PATH_SEPERATOR.length()) + ")";
        } else {
            return clazz.getCanonicalName() + "/" + method.getName() + "()";
        }


    }

    public String encode(String message, Class messageType) {
        return "{{" + message + "}}" + messageType.getCanonicalName();
    }

    public String[] decode(String message) {
        String val = message.substring(message.indexOf("{{") + 2, message.lastIndexOf("}}"));
        String type = message.substring(message.indexOf("}}") + 2, message.length());
        return new String[]{type, val};
    }
}
