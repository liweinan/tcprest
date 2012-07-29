package net.bluedash.tcprest.server;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: weli
 * Date: 7/29/12
 * Time: 3:33 AM
 * To change this template use File | Settings | File Templates.
 */
public interface TcpRestServer {

    public void up();

    public void down();

    void addResource(Class resourceClass);

    List<Class> getResourceClasses();
}
