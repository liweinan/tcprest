package net.bluedash.tcprest.invoker;

import net.bluedash.tcprest.server.Context;

/**
 *
 * @author Weinan Li
 * Jul 29 2012
 */
public interface Invoker {
    public String invoke(Context context) throws InstantiationException, IllegalAccessException;
}
