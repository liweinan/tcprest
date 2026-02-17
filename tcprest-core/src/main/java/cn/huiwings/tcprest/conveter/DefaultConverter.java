package cn.huiwings.tcprest.conveter;

import cn.huiwings.tcprest.commons.Base64;
import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.RawTypeMapper;
import cn.huiwings.tcprest.protocol.NullObj;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
    private Logger logger = LoggerFactory.getDefaultLogger();

    /**
     * Convert a method call into a string according to TcpRest protocol that can be transmitted across network.
     *
     * @param clazz   Calling class
     * @param method  Calling method
     * @param params  parameters of calling method
     * @param mappers mapper for each parameter
     * @return
     * @throws MapperNotFoundException
     */
    public String encode(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers) throws MapperNotFoundException {
        StringBuilder paramTokenBuffer = new StringBuilder();
        if (params != null) {
            for (Object param : params) {
                if (param == null)
                    param = new NullObj();
                logger.log("***DefaultConverter - encode for class: " + param.getClass());

                Mapper mapper = getMapper(mappers, param.getClass());

                paramTokenBuffer.append(encodeParam(mapper.objectToString(param))).append(TcpRestProtocol.PATH_SEPERATOR);

                logger.debug("***DefaultConverter - paramTokenBuffer " + paramTokenBuffer.toString());
            }

            return clazz.getCanonicalName() + "/" + method.getName() + "(" + paramTokenBuffer.
                    substring(0, paramTokenBuffer.length() - TcpRestProtocol.PATH_SEPERATOR.length()) + ")";
        } else {
            return clazz.getCanonicalName() + "/" + method.getName() + "()";
        }


    }

    public Object[] decode(Method targetMethod, String paramsToken, Map<String, Mapper> mappers) throws MapperNotFoundException {
        if (paramsToken == null || paramsToken.trim().length() < 1) {
            return null;
        } else {
            String rawParams[] = paramsToken.trim().split(TcpRestProtocol.PATH_SEPERATOR);
            List<Object> paramsHolder = new ArrayList<Object>();
            int i = 0;
            for (String rawParam : rawParams) {
                String thisParam = decodeParam(rawParam);
                logger.debug("param types: " + targetMethod.getParameterTypes()[i]);
                Mapper mapper = getMapper(mappers, targetMethod.getParameterTypes()[i]);

                Object param = mapper.stringToObject(thisParam);

                if (thisParam.equals(TcpRestProtocol.NULL)) {
                    param = null;
                }

                paramsHolder.add(param);
                i++;
            }
            return paramsHolder.toArray();
        }
    }

    /**
     * Encode a single parameter.
     * For example: "abc" will transform into "{{abc}}java.lang.String"
     *
     * @param message
     * @return
     */
    public String encodeParam(String message) {
        return "{{" + Base64.encode(message.getBytes()) + "}}";
    }


    /**
     * Decode a single parameter.
     *
     * @param message the encoded message in format {{base64}}
     * @return decoded string, or empty string if message is invalid
     */
    public String decodeParam(String message) {
        logger.debug("decodeParam: " + message);

        // Handle empty or null messages
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Check if message contains the expected markers
        int startIndex = message.indexOf("{{");
        int endIndex = message.lastIndexOf("}}");

        // If markers not found or in wrong order, return empty string
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            logger.warn("Invalid message format, expected {{...}}, got: " + message);
            return "";
        }

        // Extract and decode the content between {{ and }}
        String encoded = message.substring(startIndex + 2, endIndex);
        if (encoded.isEmpty()) {
            return "";
        }

        return new String(Base64.decode(encoded));
    }

    public Mapper getMapper(Map<String, Mapper> mappers, Class targetClazz) throws MapperNotFoundException {
        Mapper mapper = mappers.get(targetClazz.getCanonicalName());

        if (mapper == null) {
            // now we try to find if the target param is serizialiable, if so we could use
            // RawTypeMapper
            //                    java.io.Serializable
            for (Class clazz : targetClazz.getInterfaces()) {
                if (clazz.equals(java.io.Serializable.class) || clazz.isArray()) {
                    mapper = new RawTypeMapper();
                    break;
                }
            }

            if (mapper == null)
                throw new MapperNotFoundException("***DefaultConverter - cannot find mapper for: " + targetClazz.getCanonicalName());
        }

        logger.debug("found mapper: " + mapper.getClass().getCanonicalName() + " for: " + targetClazz.getCanonicalName());
        return mapper;
    }

    public Mapper getMapper(Map<String, Mapper> mappers, String targetClazzName) throws MapperNotFoundException {
        try {
            Mapper mapper;
            // check for array type
            if (targetClazzName.endsWith("[]")) {
                mapper = new RawTypeMapper();
                return mapper;
            }
            mapper = mappers.get(targetClazzName);
            if (mapper != null) {
                logger.debug("found mapper: " + mapper.getClass().getCanonicalName() + " for: " + targetClazzName);
                return mapper;
            } else {
                return getMapper(mappers, Class.forName(targetClazzName));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        throw new MapperNotFoundException("***DefaultConverter - cannot find mapper for: " + targetClazzName);
    }
}
