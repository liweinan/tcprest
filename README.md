# TcpRest

TcpRest is a lightweight webservice framework.

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
	HelloWorld client = factory.getInstance();

And then you could call the server like using ordinary java class:

	client.helloWorld();

TcpRest will handle all the rest of the work for you.

## Design

### Zero Dependency

TcpRest will not depended on any libraries other than JDK itself. For testing purpose it can depend on JUnit. All the components that relies on other opensource libraries will goes into *TcpRestPlus* project.

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

#### Singleton Resource

Singleton resource is registered into server by:

    tcpRestServer.addResource(new HelloWorldResource());

Unlike ordinary resources, singleton resource will only have one instance on server. So you must *ensure the thread safety* of your resource by yourself.

Note:

For ordinary resources(non-singleton), you must provide a constructor with no parameters. Here is an example:

	    public HelloWorldResource();

If you only have constructor like:

	    public HelloWorldResource(String arg);

Adding resource to server will cause problem:

	tcpRestServer.addResource(HelloWorldResource.class);

Obviously, by this registration method, you didn't provide any parameters of the resource to the server. And for non-singleton resources, each time TcpRestServer will create a new instance of the resource to serve the clients.

### TcpRestServer

TcpRestServer is the server of TcpRest. Its definition is shown as below:

	public interface TcpRestServer {
	
		public void up();
	
		public void down();
	
		void addResource(Class resourceClass);
		
		void deleteResource(Class resourceClass);
		...
	}

TcpRestServer is extensible. For example, you may use a SingleThreadTcpRestServer during project prototyping phase:

	TcpRestServer tcpRestServer = new SingleThreadTcpRestServer(8001);

And then it could be changed the it to NioTcpRestServer to increase performance in productization environment:

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

TcpRest provides mappers for following primitive data types by default:

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

You can also create your own mapper for custom data types. Suppose you have a following data type:

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

You could create a mapper for it:

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

And then register the mapper to server:

	tcpRestServer.addMapper(Color.class.getCanonicalName(), new ColorMapper());

The mapper should also be registered to client side for decoding the response from server:

	Map<String, Mapper> colorMapper = new HashMap<String, Mapper>();
	colorMapper.put(Color.class.getCanonicalName(), new ColorMapper());
	TcpRestClientFactory factory =
	        new TcpRestClientFactory(HelloWorld.class, "localhost",
	                ((SingleThreadTcpRestServer) tcpRestServer).getServerSocket().getLocalPort(), colorMapper);

#### RawTypeMapper

TcpRest supports all serializable types by RawTypeMapper and you don't have to write mapper for it. We can modify the above example to remove ColorMapper. All we need to do is to make Color serializable:

    public class Color implements Serializable {
	    ...
    }

Then TcpRest will handle the data mapping automatically. We can create a test for this:

	public interface RawType {
		public List getArrayList(List in);
	}

	public class RawTypeResource implements RawType {
		public List getArrayList(List in) {
			return in;
		}
	}

	@Test
	public void rawTypeTest() {
		// We don't put Color mapper into server,
		// so server will fallback to use RawTypeMapper to decode Color.class
		// because Color is serializable now.
		tcpRestServer.addSingletonResource(new RawTypeResource());

		TcpRestClientFactory factory =
				new TcpRestClientFactory(RawType.class, "localhost",
						((SingleThreadTcpRestServer) tcpRestServer).getServerSocket().getLocalPort());

		RawType client = (RawType) factory.getInstance();

		List lst = new ArrayList();
		lst.add(42);
		lst.add(new Color("Red"));

		List resp = client.getArrayList(lst);

		assertEquals(42, resp.get(0));

		Color c = new Color("Red");
		assertEquals(c.getName(), ((Color) resp.get(1)).getName());
	}

In above example we can see the List type is also automatically supported because all List types are implicitly serializable.

### Converter

Converter is the the reverse operation of Extractor. TcpRest client library uses it for transforming a method call into TcpRest communication protocol.

	public interface Converter {
	    public String convert(Class clazz, Method method, Object[] params);
	}

Here is a brief introduction to TcpRest's protocol:

If we make a call from client side:
	Class.method(arg1, arg2)

Tcp Rest will convert it to:

	"Class/method({{<Base64 Encoded::arg1>}}:::{{<Base64 Encoded::arg2>}})"

during network transmitting. For example:

	HelloWorldResource.sayHelloFromTo("Jack", "Lucy")

will be converted to the following string:

	"HelloWorldResource/sayHelloFromTo({{SmFjaw==}}:::{{THVjeQ==}})"

Please note that "THVjeQ==" and "SmFjaw==" are base64 encoded strings. TcpRestServer will try to find proper Mapper to decode the parameters.

## Usage Tips

### Try to make your resource class re-entrant and immutable

Try to avoid using static variables in your resource class, or you will make great effort to avoid concurrent problems. Because people may use your client interface in multi-threaded environments.

### Try to avoid using singleton resources.

If you are using singleton resources, you have to ensure the thread safety by yourself, that's a great amount of work.

