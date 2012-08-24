package io.tcprest.test.smoke;

import static org.junit.Assert.*;
import io.tcprest.annotations.ParamHandler;
import io.tcprest.annotations.SSLEnabled;
import io.tcprest.client.TcpRestClientFactory;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.server.TcpRestServer;

import java.io.IOException;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SSLEnabledSmokeTest {

	protected TcpRestServer tcpRestServer;

	@Before
	public void startTcpRestServer() throws IOException {

		tcpRestServer = new SingleThreadTcpRestServer(Math.abs(new Random()
				.nextInt()) % 10000 + 8000);
		tcpRestServer.up();
	}

	@After
	public void stopTcpRestServer() throws IOException {
		tcpRestServer.down();
	}

	@SSLEnabled(clientKeyStore = "", clientKeyStorePassword = "", clientTrustStorePassword = "", clientTrustStore = "", needClientAuth = false, serverKeyStore = "", serverKeyStorePassword = "", serverTrustStore = "", serverTrustStorePassword = "")
	private interface SSLResource {
		public String sayHello();
	}

	private class SSLResourceBean implements SSLResource {
		public String sayHello() {
			return "Hello, world!";
		}
	}

	@Test
	public void testSwitch() {
		assertTrue(ParamHandler.isSSLEnabled(SSLResource.class));
		assertTrue(ParamHandler.isSSLEnabled(SSLResourceBean.class));		
	}
	
	@Ignore
	@Test
	public void testXXX() throws IOException {

		tcpRestServer.addResource(SSLResourceBean.class);

		TcpRestClientFactory factory = new TcpRestClientFactory(
				SSLResource.class, "localhost",
				((SingleThreadTcpRestServer) tcpRestServer).getServerSocket()
						.getLocalPort());

		SSLResource client = (SSLResource) factory.getInstance();

	}
}
