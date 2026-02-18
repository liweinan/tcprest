package cn.huiwings.tcprest.example;

import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;

/**
 * Single-threaded server demonstration.
 *
 * <p>SingleThread server is ideal for:
 * <ul>
 *   <li>Development and testing</li>
 *   <li>Low-traffic production scenarios</li>
 *   <li>When SSL/TLS support is required</li>
 * </ul>
 *
 * <p><b>Features:</b>
 * <ul>
 *   <li>Blocking I/O</li>
 *   <li>SSL/TLS support</li>
 *   <li>Simple, easy to debug</li>
 * </ul>
 *
 * @author Weinan Li
 * @date 2026-02-18
 */
public class SingleThreadServerDemo {

    public static void main(String[] args) throws Exception {
        // Create server on port 8080
        TcpRestServer server = new SingleThreadTcpRestServer(8080);

        // Register service implementation
        server.addResource(HelloService.class);

        // Start server
        server.up();

        System.out.println("SingleThread server started on port 8080");
        System.out.println("Press Ctrl+C to stop");

        // Keep server running
        Thread.currentThread().join();
    }

    /**
     * Example service interface.
     */
    public interface HelloService {
        String sayHello(String name);
    }

    /**
     * Example service implementation.
     */
    public static class HelloServiceImpl implements HelloService {
        @Override
        public String sayHello(String name) {
            return "Hello, " + name + "!";
        }
    }
}
