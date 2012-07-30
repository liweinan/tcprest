package net.bluedash.tcprest.test;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class HelloWorldResource implements HelloWorld {

    public String helloWorld() {
        return "Hello, world!";
    }

    public String sayHelloTo(String name) {
        return "Hello, " + name;
    }

    public String sayHelloFromTo(String from, String to) {
        return from + " say hello to " + to;
    }

    public String oneTwoThree(String one, int two, boolean three) {
        return one + "," + Integer.valueOf(two).toString() + "," + Boolean.valueOf(three).toString();

    }


}
