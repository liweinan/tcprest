package io.tcprest.test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * For singleton resources, we must take care of the thread safety.
 * @author Weinan Li
 * @date 07 31 2012
 */
public class SingletonCounterResource implements Counter {

    private AtomicInteger counter = new AtomicInteger();

    public SingletonCounterResource() {
        this(0);
    }

    public SingletonCounterResource(int i) {
        this.counter.set(i);
    }

    public int getCounter() {
        return counter.get();
    }

    public void increaseCounter() {
        this.counter.incrementAndGet();
    }
}
