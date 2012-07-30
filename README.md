# TcpRest

## Motivation

I want to write a tcp server that could automatically map java classes into network apis. For example, here is a java class:

    public class HelloWorldRestlet {

        public String helloWorld() {
            return "Hello, world!";
        }
    }

I want an easy method to register this class into a TCP server, somthing like:

	TcpRestServer tcpRestServer = new SingleThreadTcpRestServer(8001);
	tcpRestServer.up();
	tcpRestServer.addResource(HelloWorldRestlet.class);


And from client side I could call 'helloWorld' method like calling the local java method:

	TcpRestClientFactory factory = new TcpRestClientFactory(HelloWorld.class, "localhost", 8001);
	HelloWorld client = (HelloWorld) factory.getInstance();
	client.helloWorld();

TcpRest will handle all the rest of the work to me.

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

TcpRest will help to map this class to a tcp webservice and make it work.

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

It could also be removed at runtime:

	tcpRestServer.deleteResource(AnotherRestlet.class);

### Extractor

TcpRestServer needs Extractor to process clients request and map the incoming string data into method call. TcpRestServer needs to be smart enough to know that user wants to call. The Extractor's role is to process client's request and generate a call context:

	Context context = extractor.extract(request);

### Context

Context is an object that holds all of the information provided by incoming request:

	public class Context {
	    private Class targetClazz;
	    private Method targetMethod;
	    private List<Object> params;
	    ...
	}

### Invoker

Invoker will call the class method inside its invoke() method and return the response object to TcpRestServer.

	public interface Invoker {
	    public Object invoke(Context context);
	}

TcpRestServer will use the returned object from invoker and transform it into string response to client.

We can put all the knowledge we've learnt above into SingleThreadTcpRestServer:

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

The above pseudocode show how the server work: It uses extractor to convert request from string to context object, and then invoker will invoke the relative method in mapped class and return back the response to server. Finally the server write response to client.

But with only the above codes, we can only trasmitting string values between server and client, that's not enough to map a java class into a network based communication, because network can only send/receive strings instead of java objects. So we need some tools to do the data transformation to us. The following two components are for this purpose:

### Mapper

...

### Converter

Converter is a low-level tool that TcpRest use it to process its own protocol. Here is a brief introduction to TcpRest's protocol:

	TcpRest Protocol:
		Class.method(arg1, arg2) will transform to:
			"Class/method({{arg1}}arg1ClassName,{{arg2}}arg2ClassName)"
		
	For example:
	HelloWorldRestlet.sayHelloFromTo("Jack", "Lucy") will be converted to the following string:
	"HelloWorldRestlet/sayHelloFromTo({{Jack}}java.lang.String,{{Lucy}}java.lang.String)"

The definition of Converter is shown as below:

	public interface Converter {
	    public String convert(Class clazz, Method method, Object[] params);
	}


## Requirement

### Zero Dependency

TcpRest should not depend on any other projects other than JDK itself, nevertheless it depends on JUnit4 for testing.

*In the future I'd like to create an TcpRestPlus project to collect all the add-ons that depend on third-party libraries*

### Automatic serialization and de-serialization

User shouldn't be forced to understand the underlying communication protocol during network communication. They should transparently use the client library to access the server.

## FAQ

### Why another webservice framework?

Because I really want to have a framework that's easy to use and doesn't rely on HTTP or any servlet container.

### Why you don't use Apache CXF, JBoss RESTEasy, Jersey or any other JAX-RS framework?

I don't want to be forced to use HTTP protocol get the job done.

### Why don't you use the Java serializations scheme or some other RPC frameworks such as COBRA?

It's all about simplicity. The goal for TcpRest is:

* Using 5 lines of code to transfer your java class into both server and client!

Suppose you have a java class:

	public interface HelloWorld {
		public String helloWorld();
		public String sayHelloTo(String name);
	}

	public class HelloWorldRestlet implements HelloWorld {

	    public String helloWorld() {
	        return "Hello, world!";
	    }

	    public String sayHelloTo(String name) {
	        return "Hello, " + name;
	    }

	}

Here are the three lines of code that transfer the above class into a server:

	TcpRestServer tcpRestServer = new SingleThreadTcpRestServer(8001);
	tcpRestServer.up();
	tcpRestServer.addResource(HelloWorldRestlet.class);


And here are the two lines of code for client:

	TcpRestClientFactory factory = 
		new TcpRestClientFactory(HelloWorld.class, "localhost", 8001);
	HelloWorld client = (HelloWorld) factory.getInstance();

That's all. Now you can use the client to call the server:

	client.helloWorld();

This is the goal the TcpRestServer want to achieve.

### What's the differences between TcpRest other RPC frameworks such as gSOAP or Apache Thrift, e.g.?

gSOAP and Apache Thrift generates code for you, it's more on 'compiling and run' side, but TcpRest is all about runtime. You can add/remove resources at runtime:

	tcpRestServer.addResource(HelloWorldRestlet.class);
	tcpRestServer.deleteResource(AnotherRestlet.class);

*You don't have to generate any code, TcpRest generates code for you.*


## TODO

* Provide client library
* Implement ThreadPoolRestServer
* Implement NioTcpRestServer
* Implement JsonMapper
* Support Restlet annotation
* Make DefaultExtractor supports parentheses in parameter.
* Support SSL communication


