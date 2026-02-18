package cn.huiwings.tcprest.mapper;

import cn.huiwings.tcprest.protocol.TcpRestProtocol;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class NullMapper implements Mapper {
    public Object stringToObject(String param) {
        return null;
    }

    public String objectToString(Object object) {
        return TcpRestProtocol.NULL;
    }

}
