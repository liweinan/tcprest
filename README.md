# Tcp Rest

I'd like to write a tcp server that could automatically map java classes into network apis. For example, here is a java class:

    public class HelloWorldRestlet {

        public String helloWorld() {
            return "Hello, world!";
        }
    }

I want an easy method to register this class into a TCP server, somthing like:

	TcpRestServer tcpRestServer = new SimpleTcpRestServer(8001);
	tcpRestServer.up();
	tcpRestServer.addResource(HelloWorldRestlet.class);

And from client side I could call 'helloWorld' method by sending api call request:

    "HelloWorldRestlet/helloWorld"

Sending the class name and the method name with above format to server:

	Socket clientSocket = new Socket("localhost", 8001);
	PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
	BufferedReader reader =
	        new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	writer.println("HelloWorldRestlet/helloWorld");
	writer.flush();

And I could get the response:

	String response = reader.readLine();
	assertEquals("Hello, world!", response);

You may feel the concept behind is very much like JAX-RS frameworks such as JBoss CXF, RESTEasy or Jersey, and yes you are correct :-) Actually I'm currently the maintainer of RESTEasy for JBoss EAP, and I'm writing this project for fun. It's based on TCP instead of HTTP, and I hope this project could have some real values to the practical work in the far future ;-)

## Design

...

## Requirement

I'd like the project to meet the following requirements:

### Zero Dependency

It won't depends on any other projects other than JDK itself.

...


