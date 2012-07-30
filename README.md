# TcpRest

---

## Introduction

TcpRest is a framework that can turn your POJO into a server/client pair. Suppose you have a java class like this:

    public class HelloWorldRestlet {

        public String helloWorld() {
            return "Hello, world!";
        }
    }

It needs three lines of code to turn it into a server:

	TcpRestServer tcpRestServer = new SingleThreadTcpRestServer(8001);
	tcpRestServer.up();
	tcpRestServer.addResource(HelloWorldRestlet.class);


For client side it needs two lines of code:

	TcpRestClientFactory factory = 
		new TcpRestClientFactory(HelloWorld.class, "localhost", 8001);
	HelloWorld client = (HelloWorld) factory.getInstance();

And then you could call the server like using ordinary java class:

	client.helloWorld();

TcpRest will handle all the rest of the work to you.

## Design

The system contains several core components:

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


## TODO

* Provide client library
* Implement ThreadPoolRestServer
* Implement NioTcpRestServer
* Implement JsonMapper
* Support Restlet annotation
* Make DefaultExtractor supports parentheses in parameter.
* Support SSL communication


