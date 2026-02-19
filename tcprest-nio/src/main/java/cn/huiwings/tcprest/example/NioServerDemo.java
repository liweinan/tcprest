package cn.huiwings.tcprest.example;

import cn.huiwings.tcprest.classloader.FilePathClassLoader;
import cn.huiwings.tcprest.server.NioTcpRestServer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Use following command to start:
 * <pre>
 * mvn -q exec:java -Dexec.mainClass="io.tcprest.server.NioTcpRestServer"
 * </pre>
 */
public class NioServerDemo {
    public static void main(String args[]) {
        NioTcpRestServer server = null;
        try {
            server = new NioTcpRestServer();
            ClassLoader cl = new FilePathClassLoader("/Users/weli/projs/tcprest/target/test-classes/");
            Class resourceClass = cl.loadClass("io.tcprest.test.HelloWorldResource");
            server.addResource(resourceClass);
            // Disable logging for demo
            Logger.getLogger(NioTcpRestServer.class.getName()).setLevel(Level.OFF);
            server.up(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
