package net.bluedash.tcprest.test;

/**
*
* @author Weinan Li
* CREATED AT: Jul 29 2012
*/
public class HelloWorldRestlet {

    public String helloWorld() {
        return "Hello, world!";
    }

    public String sayHelloTo(String name) {
        return "Hello, " + name;
    }

}
