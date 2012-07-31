# TcpRest

TcpRest is a network framework.

## Introduction

TcpRest is a framework that can turn your POJO into a server/client pair. Suppose you have a java class like this:

    public class HelloWorldResource {

        public String helloWorld() {
            return "Hello, world!";
        }
    }

It needs three lines of code to turn it into a server:

	TcpRestServer tcpRestServer = new SingleThreadTcpRestServer(8001);
	tcpRestServer.up();
	tcpRestServer.addResource(HelloWorldResource.class);


For client side it needs two lines of code:

	TcpRestClientFactory factory = 
		new TcpRestClientFactory(HelloWorld.class, "localhost", 8001);
	HelloWorld client = (HelloWorld) factory.getInstance();

And then you could call the server like using ordinary java class:

	client.helloWorld();

TcpRest will handle all the rest of the work for you.

## Design

The system contains several core components:

### Resource

Resource is the plain java class that you want to turn to a network API. Here is an example:

	public class HelloWorldResource {

	    public String helloWorld() {
	        return "Hello, world!";
	    }

	}

You can turn it into server by:

	TcpRestServer tcpRestServer = new SingleThreadTcpRestServer(8001);
	tcpRestServer.up();
	tcpRestServer.addResource(HelloWorldResource.class);

### TcpRestServer

TcpRestServer is the server of TcpRest. Its definition is shown as below:

	public interface TcpRestServer {
	
		public void up();
	
		public void down();
	
		void addResource(Class resourceClass);
		
		void deleteResource(Class resourceClass);
		...
	}

* TcpRestServer is extensible

For example, we may use a SingleThreadTcpRestServer during project prototyping phase:

	TcpRestServer tcpRestServer = new SingleThreadTcpRestServer(8001);

And then we could change the it to NioTcpRestServer to increase performance in productization environment:

	TcpRestServer tcpRestServer = new NioTcpRestServer(8001);

In addition, TcpRest should allow developers to write their own implementations of TcpRestServer.

* Resource could be registered into server at runtime

The framework should allow users to register new webservices at runtime:

	tcpRestServer.addResource(someResourceClass);

It could also be removed at runtime:

	tcpRestServer.deleteResource(someResourceClass);

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

Mapper is the core component doing data serialization and deserialization. Here is its definition:

	public interface Mapper {
		public Object stringToObject(String param);
	
		public String objectToString(Object object);
	}

TcpRest provides mappers for follwing primitive data types by default:

* byte
* short
* int
* long
* float
* double
* String
* boolean

For example:

	public class IntegerMapper implements Mapper {

	    public Object stringToObject(String param) {
	        return Integer.valueOf(param);
	    }

	    public String objectToString(Object object) {
	        return object.toString();
	    }
	}

You can also create your own mapper for custom data types. Suppose we have a following data type:

	public class Color {
	    private String name;

	    public Color(String name) {
	        this.name = name;
	    }

	    public String getName() {
	        return name;
	    }

	    public void setName(String name) {
	        this.name = name;
	    }
	}

We could create a mapper for it:

	public class ColorMapper implements Mapper {
	    public Object stringToObject(String param) {
	        return new Color(param);
	    }

	    public String objectToString(Object object) {
	        if (object instanceof Color) {
	            return ((Color) object).getName();
	        } else {
	            return null;
	        }

	    }
	}

Then we could register the mapper to server:

	tcpRestServer.addMapper(Color.class.getCanonicalName(), new ColorMapper());

We should also register it to client side:

	Map<String, Mapper> colorMapper = new HashMap<String, Mapper>();
	colorMapper.put(Color.class.getCanonicalName(), new ColorMapper());
	TcpRestClientFactory factory =
	        new TcpRestClientFactory(HelloWorld.class, "localhost",
	                ((SingleThreadTcpRestServer) tcpRestServer).getServerSocket().getLocalPort(), colorMapper);

Then we could use Color class in our API:

	public interface HelloWorld {
	    public String favoriteColor(Color color);
	}

### Converter

Converter is a low-level tool that TcpRest use it to process its own protocol. The definition of Converter is shown as below:

	public interface Converter {
	    public String convert(Class clazz, Method method, Object[] params);
	}

Here is a brief introduction to TcpRest's protocol:

If we make a call from client side:
	Class.method(arg1, arg2)

Tcp Rest will convert it to:

	"Class/method({{arg1}}arg1ClassName,{{arg2}}arg2ClassName)"

during network transmitting. For example:

	HelloWorldResource.sayHelloFromTo("Jack", "Lucy")

will be converted to the following string:

	"HelloWorldResource/sayHelloFromTo({{Jack}}java.lang.String,{{Lucy}}java.lang.String)"

TcpRestServer will try to find proper Mapper to decode the parameters.
