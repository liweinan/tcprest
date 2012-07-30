package net.bluedash.tcprest.test;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class HelloWorldRestlet implements HelloWorld {

    public String helloWorld() {
        return "Hello, world!";
    }

    public String sayHelloTo(String name) {
        return "Hello, " + name;
    }

    public String sayHelloFromTo(String from, String to) {
        return from + " say hello to " + to;
    }


}
