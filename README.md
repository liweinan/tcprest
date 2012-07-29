# TCP REST

## Motivation

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

## TCP is not HTTP

You may feel the concept behind is very much like JAX-RS frameworks such as JBoss CXF, RESTEasy or Jersey, and yes you are correct :-) 

Actually I'm currently the maintainer of RESTEasy for JBoss EAP, so I've borrowed a lot of concepts from HTTP JAX-RS framework to work on this project. But this project is based on TCP instead of HTTP, which will not using HTTP protocol to do the communication. 

Alternatively, I'll define a lightweight protocol by myself for data exchanging. That means this project will not grow to a full fledged framework that can take any workload, but it could be very suitable to achieve some small work.

## Goal

Now I'm writing this project just for fun, and the development speed could be slow but I'll keep working on it. I hope this project could have some real values to the practical work in the far future ;-)

## Design

The system contains several core concepts:

### Restlet

Restlet is the java class the developers will write as webservice. For example:

	public class HelloWorldRestlet {

	    public String helloWorld() {
	        return "Hello, world!";
	    }

	}

And the class should be easily registered into Server:

	tcpRestServer.addResource(HelloWorldRestlet.class);

And TcpRest will help to map this class to a tcp webservice and make it work.

### TcpRestServer

TcpRestServer uses SocketServer to do the TCP communication, and it should achieve all of the following goals:

* It should be extensible

For example, we may use a SingleThreadTcpRestServer during project prototyping phase:

	TcpRestServer tcpRestServer = new SingleThreadTcpRestServer(8001);

And then we could change the it to NioTcpRestServer to increase performance in productization environment:

	TcpRestServer tcpRestServer = new NioTcpRestServer(8001);

In addition, TcpRest should allow developers to write their own implementations of TcpRestServer.

* Resources could be registered into server at runtime

The framework should allow users to register new webservices at runtime:

	tcpRestServer.addResource(AnotherRestlet.class);

And then the clients could access this new service by sending request:

	"AnotherRestlet/someMethod(arg1, arg2, ...)"

...

### Extractor

TcpRestServer needs Extractor to process clients request and map the incoming string data into method call. For example, if the user requests:

	"AnotherRestlet/someMethod(arg1, arg2, ...)"

TcpRestServer needs to be smart enough to know that user wants to call:

	AnotherRestlet.someMethod(arg1, arg2)

The Extractor's role is to process client's request and generate a call context:

	Context context = extractor.extract(request);

### Context

Context is an object that holds all of the information provided by incoming request:

	public class Context {
	    private Class targetClazz;
	    private Method targetMethod;
	    private List<Object> params;
	    ...
	}

For example, if the user requests:

	"AnotherRestlet/someMethod(arg1, arg2, ...)"

Extractor will help to extract this request and put the info into Context:

	targetClazz -> AnotherRestlet.class
	targetMethod -> someMethod()
	params -> arg1, arg2...

After we've gotten all these information, we could use Invoker to do the real work.

### Invoker

Invoker has an invoke() method, and it uses Context to take the real work:

	public interface Invoker {
	    public Object invoke(Context context);
	}

It will call the class method inside its invoke() method and return the response object to TcpRestServer. And then TcpRestServer could process this response and return proper data to client.

So we can put all the knowledge we've learnt above and read the following code in SingleThreadTcpRestServer:

	while (status.equals(TcpRestServerStatus.RUNNING)) {
	    logger.log("Server started.");
	    Socket socket = serverSocket.accept();
	    logger.log("Client accepted.");
	    BufferedReader reader = 
	    	new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    Scanner scanner = new Scanner(reader);

	    PrintWriter writer = new PrintWriter(socket.getOutputStream());
	    while (scanner.hasNext()) {
	        String request = scanner.nextLine();
	        logger.log("request: " + request);
	        // extract calling class and method from request
	        Context context = extractor.extract(request);
	        // invoke real method
	        String response = (String) invoker.invoke(context);
	        writer.println(response);
	        writer.flush();
	    }
	}

As the code show above, the server will accept user request, using extractor to convert request from string to context object, and then invoker will invoke the relative method in mapped class and return back the response to server. Finally the server write response to client.

## Example

Here is an example how to use the framework:

	public class TcpServerSmokeTests {

	    private TcpRestServer tcpRestServer;
	    private Socket clientSocket;


	    @Before
	    public void startTcpRestServer() throws IOException {
	        tcpRestServer = new SingleThreadTcpRestServer(8001);
	        tcpRestServer.up();
	        clientSocket = new Socket("localhost", 8001);
	    }

	    @After
	    public void stopTcpRestServer() throws IOException {
	        tcpRestServer.down();
	        clientSocket.close();
	    }

	    @Test
	    public void testSimpleClient() throws IOException {
	        tcpRestServer.addResource(HelloWorldRestlet.class);

	        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
	        BufferedReader reader =
	                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	        writer.println("net.bluedash.tcprest.test.HelloWorldRestlet/helloWorld");
	        writer.flush();

	        String response = reader.readLine();
	        assertEquals("Hello, world!", response);

	    }

	}


## Requirement

### Zero Dependency

TcpRest should not depend on any other projects other than JDK itself. Only for testing it could depend on JUnit.

...


## TODO

