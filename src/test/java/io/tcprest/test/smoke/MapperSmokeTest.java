package io.tcprest.test.smoke;

import io.tcprest.client.TcpRestClientFactory;
import io.tcprest.mapper.RawTypeMapper;
import io.tcprest.mapper.Mapper;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.test.Color;
import io.tcprest.test.ColorMapper;
import io.tcprest.test.HelloWorld;
import io.tcprest.test.HelloWorldResource;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class MapperSmokeTest extends TcpClientFactorySmokeTest {

	@Test
	public void extendMapperTest() {
		tcpRestServer.addResource(HelloWorldResource.class);
		tcpRestServer.addMapper(Color.class.getCanonicalName(),
				new ColorMapper());

		Map<String, Mapper> colorMapper = new HashMap<String, Mapper>();
		colorMapper.put(Color.class.getCanonicalName(), new ColorMapper());

		TcpRestClientFactory factory = new TcpRestClientFactory(
				HelloWorld.class, "localhost",
				tcpRestServer.getServerPort(), colorMapper);

		HelloWorld client = (HelloWorld) factory.getInstance();
		Color color = new Color("Red");
		assertEquals("My favorite color is: Red", client.favoriteColor(color));

	}

	@Test
	public void testArrayListMapper() {
		List l = new ArrayList();
		l.add(1);
		l.add(2);
		l.add(3);
		RawTypeMapper mapper = new RawTypeMapper();
		System.out.println(mapper.objectToString(l));
	}

	public interface RawType {
		public List getArrayList(List in);

		public HashMap<String, List<Color>> getComplexType(
				HashMap<String, List<Color>> in);
	}

	public class RawTypeResource implements RawType {
		public List getArrayList(List in) {
			return in;
		}

		public HashMap<String, List<Color>> getComplexType(
				HashMap<String, List<Color>> in) {
			return in;
		}
	}

	@SuppressWarnings({"unchecked","rawtypes"})	
	@Test
	public void rawTypeTest() {
		// We don't put Color mapper into server,
		// so server will fallback to use RawTypeMapper to decode Color.class
		// because Color is serializable now.
		tcpRestServer.addSingletonResource(new RawTypeResource());

		TcpRestClientFactory factory = new TcpRestClientFactory(RawType.class,
				"localhost", ((SingleThreadTcpRestServer) tcpRestServer)
						.getServerPort());

		RawType client = (RawType) factory.getInstance();
		Color red = new Color("Red");

		{			
			List request = new ArrayList();
			request.add(42);
			request.add(red);

			List response = client.getArrayList(request);

			assertEquals(42, response.get(0));
			assertEquals(red.getName(), ((Color) response.get(1)).getName());
		}

		{
			List<Color> list = new ArrayList<Color>();
			list.add(red);
			HashMap<String, List<Color>> request = new HashMap<String, List<Color>>();
			request.put("item", list);
			
			HashMap<String, List<Color>> response = client.getComplexType(request);
			
			assertEquals("item", response.keySet().iterator().next());
			assertEquals(red.getName(), response.get("item").get(0).getName());
		}

	}
}
