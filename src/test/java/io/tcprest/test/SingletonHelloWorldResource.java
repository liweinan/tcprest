package io.tcprest.test;

import io.tcprest.annotations.Singleton;

@Singleton
public class SingletonHelloWorldResource implements HelloWorld {
    @Override
    public String helloWorld() {
        return null;
    }

    @Override
    public String sayHelloTo(String name) {
        return null;
    }

    @Override
    public String sayHelloFromTo(String from, String to) {
        return null;
    }

    @Override
    public String oneTwoThree(String one, int two, boolean three) {
        return null;
    }

    @Override
    public String favoriteColor(Color color) {
        return null;
    }

    @Override
    public String allTypes(String one, int two, boolean three, short x, long y, double z, byte o) {
        return null;
    }

    @Override
    public String timeout() {
        return null;
    }

    @Override
    public String[] getArray(String[] in) {
        return new String[0];
    }

    @Override
    public String echo(String in) {
        return null;
    }
}
