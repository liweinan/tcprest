package cn.huiwings.tcprest.test.smoke;

import java.util.Random;

/**
 * 11 05 2012
 *
 * @author <a href="mailto:l.weinan@gmail.com">Weinan Li</a>
 */
public class PortGenerator {

    public static int get() {
        return Math.abs((new Random()).nextInt()) % 10000 + 8000;
    }
}
