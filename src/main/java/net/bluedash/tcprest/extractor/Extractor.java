package net.bluedash.tcprest.extractor;

import net.bluedash.tcprest.exception.MapperNotFoundException;
import net.bluedash.tcprest.exception.ParseException;
import net.bluedash.tcprest.server.Context;

/**
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public interface Extractor {
    public Context extract(String request) throws ClassNotFoundException, NoSuchMethodException, ParseException, MapperNotFoundException;
}
