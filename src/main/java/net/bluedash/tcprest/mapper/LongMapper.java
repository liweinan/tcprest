package net.bluedash.tcprest.mapper;

/**
 * @author Weinan Li
 * @date 07 30 2012
 */
public class LongMapper implements Mapper {

    public Object stringToObject(String param) {
        return Long.valueOf(param);
    }

    public String objectToString(Object object) {
        return object.toString();
    }
}
