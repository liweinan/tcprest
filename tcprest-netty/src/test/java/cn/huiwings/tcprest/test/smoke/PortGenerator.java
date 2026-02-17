package cn.huiwings.tcprest.test.smoke;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Port generator for tests. Generates sequential ports to avoid conflicts.
 *
 * <p>Supports both global port generation and dedicated port ranges per test class.</p>
 *
 * @author Weinan Li
 * @date 11 05 2012
 */
public class PortGenerator {

    private static final AtomicInteger counter = new AtomicInteger(20000);

    /**
     * Get next global port (legacy method).
     *
     * @return next available port
     */
    public static int get() {
        return counter.getAndIncrement();
    }

    /**
     * Create a dedicated port range starting from the specified base port.
     *
     * <p>Usage:</p>
     * <pre>
     * private static final PortRange portRange = PortGenerator.from(30000);
     *
     * int port = portRange.next();
     * </pre>
     *
     * @param basePort the base port number
     * @return a new PortRange instance
     */
    public static PortRange from(int basePort) {
        return new PortRange(basePort);
    }

    /**
     * Port range for dedicated test class port allocation.
     */
    public static class PortRange {
        private final AtomicInteger counter;

        private PortRange(int basePort) {
            this.counter = new AtomicInteger(basePort);
        }

        /**
         * Get next port in this range.
         *
         * @return next available port
         */
        public int next() {
            return counter.getAndIncrement();
        }
    }
}
