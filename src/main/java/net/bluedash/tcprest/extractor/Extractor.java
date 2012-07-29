package net.bluedash.tcprest.extractor;

import net.bluedash.tcprest.server.Context;

/**
 * Created by IntelliJ IDEA.
 * User: weli
 * Date: 7/29/12
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Extractor {
    public Context extract(String request) throws ClassNotFoundException, NoSuchMethodException;
}
