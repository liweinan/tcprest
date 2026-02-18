package cn.huiwings.tcprest.test.smoke;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Port generator for tests.
 *
 * <p>Generates sequential port numbers starting from a base port.
 * Each test class should use a different base port to avoid conflicts.</p>
 *
 * <p>Recommended port ranges:</p>
 * <ul>
 *   <li>Smoke tests: 20000-20999</li>
 *   <li>Integration tests: 21000-22999</li>
 *   <li>Performance tests: 23000-23999</li>
 * </ul>
 *
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
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
