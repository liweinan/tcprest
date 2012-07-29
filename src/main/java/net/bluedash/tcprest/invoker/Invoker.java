package net.bluedash.tcprest.invoker;

import net.bluedash.tcprest.server.Context;

/**
 * Created by IntelliJ IDEA.
 * User: weli
 * Date: 7/29/12
 * Time: 3:46 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Invoker {
    public String invoke(Context context) throws InstantiationException, IllegalAccessException;
}
