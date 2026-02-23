package cn.huiwings.tcprest.test.smoke;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Port generator for tcprest-registry tests. Uses base 26000+ to avoid conflict with other modules.
 */
public final class PortGenerator {

    private static final AtomicInteger counter = new AtomicInteger(26000);

    public static int get() {
        return counter.getAndIncrement();
    }
}
