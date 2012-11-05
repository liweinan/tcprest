package io.tcprest.example;

import io.tcprest.classloader.FilePathClassLoader;
import io.tcprest.logger.NullLogger;
import io.tcprest.server.NioTcpRestServer;

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
            server.setLogger(new NullLogger());
            server.up(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
