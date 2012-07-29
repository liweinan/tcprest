# TcpRest

I'd like to write a tcp server that could automatically map java classes into network apis. For example, here is a java class:

    public class HelloWorldRestlet {

        public String helloWorld() {
            return "Hello, world!";
        }
    }

I want a easy method to register this class into a TCP server, somthing like:

	TcpRestServer tcpRestServer = new SimpleTcpRestServer(8001);
	tcpRestServer.up();
	tcpRestServer.addResource(HelloWorldRestlet.class);

And from client side I could call 'helloWorld' method by sending api call request:

    "HelloWorldRestlet/helloWorld"

Sending the class name and the method name with above format to server, so I could call the auto-mapped server api:

	Socket clientSocket = new Socket("localhost", 8001);
	PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
	BufferedReader reader =
	        new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	writer.println("HelloWorldRestlet/helloWorld");
	writer.flush();

	String response = reader.readLine();
	assertEquals("Hello, world!", response);

## Requirement

I'd like the project to meet the following requirements:

### Zero Dependency

It won't depends on any other projects other than JDK itself.

...


