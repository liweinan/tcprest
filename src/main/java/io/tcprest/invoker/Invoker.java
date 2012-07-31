package io.tcprest.invoker;

import io.tcprest.server.Context;

/**
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public interface Invoker {
    public Object invoke(Context context) throws InstantiationException, IllegalAccessException;
}
