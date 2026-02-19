package cn.huiwings.tcprest.test.smoke;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Port generator for tests. Generates sequential ports to avoid conflicts.
 *
 * <p>Supports both global port generation and dedicated port ranges per test class.
 * Each module/test class may use {@link #from(int)} for an isolated range.</p>
 *
 * <p>Recommended port ranges (per CLAUDE.md):</p>
 * <ul>
 *   <li>tcprest-commons: 8000+</li>
 *   <li>tcprest-singlethread: 14000+, e.g. integration 21000, bind 26000</li>
 *   <li>tcprest-netty: 20000+, e.g. bind 30000</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 * int port = PortGenerator.get();
 * // or dedicated range:
 * private static final PortRange portRange = PortGenerator.from(21000);
 * int port = portRange.next();
 * </pre>
 *
 * @author Weinan Li
 */
public class PortGenerator {

    // Default base port for backward compatibility (high range to avoid system services)
    private static final AtomicInteger counter = new AtomicInteger(20000);

    /**
     * Get next port from default range (20000+).
     *
     * @return next available port number
     */
    public static int get() {
        return counter.getAndIncrement();
    }

    /**
     * Get next port from custom base range.
     * Useful for test classes that need isolated port ranges.
     *
     * @param basePort starting port number (recommended: 20000-30000)
     * @return generator for the specified base port
     */
    public static PortRange from(int basePort) {
        return new PortRange(basePort);
    }

    /**
     * Port range generator starting from a specific base port.
     */
    public static class PortRange {
        private final AtomicInteger counter;

        private PortRange(int basePort) {
            this.counter = new AtomicInteger(basePort);
        }

        /**
         * Get next port from this range.
         *
         * @return next available port number
         */
        public int next() {
            return counter.getAndIncrement();
        }
    }
}
