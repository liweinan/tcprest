package cn.huiwings.tcprest.mapper;

/**
 * Mapper can encode incoming string request to complex datatype
 *
 * @author Weinan Li
 * Jul 30 2012
 */
public interface Mapper {
    public Object stringToObject(String param);

    public String objectToString(Object object);
}
