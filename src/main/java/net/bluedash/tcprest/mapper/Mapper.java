package net.bluedash.tcprest.mapper;

/**
 * Mapper can convert incoming string request to complex datatype
 *
 * @author Weinan Li
 * Jul 30 2012
 */
public interface Mapper {
    public Object stringToObject(String param);

    public String objectToString(Object object);
}
