package cn.huiwings.tcprest.extractor;

import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.exception.ParseException;
import cn.huiwings.tcprest.server.Context;

/**
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public interface Extractor {
    public Context extract(String request) throws ClassNotFoundException, NoSuchMethodException, ParseException, MapperNotFoundException;
}
