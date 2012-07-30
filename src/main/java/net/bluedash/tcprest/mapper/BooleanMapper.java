package net.bluedash.tcprest.mapper;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class BooleanMapper implements Mapper {
    public Object stringToObject(String param) {
        return Boolean.valueOf(param);
    }

    public String objectToString(Object object) {
        return object.toString();
    }

}
