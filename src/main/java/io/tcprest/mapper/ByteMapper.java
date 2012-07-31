package io.tcprest.mapper;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class ByteMapper implements Mapper {
    public Object stringToObject(String param) {
        return Byte.valueOf(param);
    }

    public String objectToString(Object object) {
        return object.toString();
    }

}
