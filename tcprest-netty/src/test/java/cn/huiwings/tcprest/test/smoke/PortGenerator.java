package cn.huiwings.tcprest.test.smoke;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 11 05 2012
 *
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
 */
public class PortGenerator {

    private static final AtomicInteger counter = new AtomicInteger(20000);

    public static int get() {
        return counter.getAndIncrement();
    }
}
