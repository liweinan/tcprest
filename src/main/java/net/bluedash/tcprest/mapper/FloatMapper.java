package net.bluedash.tcprest.mapper;

/**
 * @author Weinan Li
 * @date 07 30 2012
 */
public class FloatMapper implements Mapper {

    public Object stringToObject(String param) {
        return Float.valueOf(param);
    }

    public String objectToString(Object object) {
        return object.toString();
    }
}
