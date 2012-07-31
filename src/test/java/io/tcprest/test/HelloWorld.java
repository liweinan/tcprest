package io.tcprest.test;

/**
 * @author Weinan Li
 * @date Jul 30 2012
 */
public interface HelloWorld {
    public String helloWorld();

    public String sayHelloTo(String name);

    public String sayHelloFromTo(String from, String to);

    public String oneTwoThree(String one, int two, boolean three);

    public String favoriteColor(Color color);

    public String allTypes(String one, int two, boolean three, short x, long y, double z, byte o);
}