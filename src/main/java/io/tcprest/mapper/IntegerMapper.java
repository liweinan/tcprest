package io.tcprest.mapper;

/**
 * @author Weinan Li
 * @date 07 30 2012
 */
public class IntegerMapper implements Mapper {

    public Object stringToObject(String param) {
        return Integer.valueOf(param);
    }

    public String objectToString(Object object) {
        return object.toString();
    }
}
