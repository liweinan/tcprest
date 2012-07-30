package net.bluedash.tcprest.conveter;

import net.bluedash.tcprest.logger.Logger;
import net.bluedash.tcprest.logger.SystemOutLogger;

import java.lang.reflect.Method;

/**
 * TcpRest Protocol:
 * Class.method(arg1, arg2) should transform to: "Class/method({{arg1}}arg1ClassName,{{arg2}}arg2ClassName)"
 * <p/>
 * For example:
 * HelloWorldRestlet.sayHelloFromTo("Jack", "Lucy") should transform to:
 * "HelloWorldRestlet/sayHelloFromTo({{Jack}}java.lang.String,{{Lucy}}java.lang.String)"
 *
 * @author Weinan Li
 * @date 07 31 2012
 */
public class DefaultConverter implements Converter {
    Logger logger = new SystemOutLogger();

    public String convert(Class clazz, Method method, Object[] params) {
        StringBuilder paramTokenBuffer = new StringBuilder();
        if (params != null) {
            for (Object param : params) {
                logger.log("***DefaultConverter: " + param.getClass());
                paramTokenBuffer.append("{{").append(param.toString()).append("}}").append(param.getClass().getCanonicalName()).append(",");
            }
            paramTokenBuffer.deleteCharAt(paramTokenBuffer.length() - 1); // delete the last ','
            return clazz.getCanonicalName() + "/" + method.getName() + "(" + paramTokenBuffer.toString() + ")";
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
