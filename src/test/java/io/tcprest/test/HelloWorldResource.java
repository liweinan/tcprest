package io.tcprest.test;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class HelloWorldResource implements HelloWorld {

    public HelloWorldResource() {
    }

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

    public String favoriteColor(Color color) {
        return "My favorite color is: " + color.getName();
    }

    public String allTypes(String one, int two, boolean three, short x, long y, double z, byte o) {
        return one + "," + Integer.valueOf(two).toString() + "," + Boolean.valueOf(three).toString() + Short.valueOf(x) + Long.valueOf(y) + Double.valueOf(z) + Byte.valueOf(o);

    }

    public String timeout() {
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return "ok";
    }

    public String[] getArray(String[] in) {
        return in;
    }


}
