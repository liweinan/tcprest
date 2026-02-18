package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.converter.Converter;
import cn.huiwings.tcprest.converter.DefaultConverter;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class SimpleTcpServerSmokeTest {

    protected TcpRestServer tcpRestServer;
    protected Socket clientSocket;


    @BeforeMethod
    public void startTcpRestServer() throws Exception {
        int port = Math.abs(new Random().nextInt()) % 10000 + 8000;
        tcpRestServer = new SingleThreadTcpRestServer(port);
        tcpRestServer.up();
        clientSocket = new Socket("localhost", port);
    }

    @AfterMethod
    public void stopTcpRestServer() throws IOException {
        tcpRestServer.down();
        clientSocket.close();
    }

    // Helper to strip compression prefix from response
    private String stripCompressionPrefix(String response) {
        if (response != null && (response.startsWith("0|") || response.startsWith("1|"))) {
            return response.substring(2);
        }
        return response;
    }

    @Test
    public void testSimpleClient() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);
        Converter converter = new DefaultConverter();

        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // New secure format: 0|{{base64(meta)}}|{{base64(params)}}
        String meta = "cn.huiwings.tcprest.test.HelloWorldResource/helloWorld";
        String metaBase64 = ProtocolSecurity.encodeComponent(meta);
        String paramsBase64 = ProtocolSecurity.encodeComponent(""); // Empty params
        String request = "0|" + metaBase64 + "|" + paramsBase64;

        writer.println(request);
        writer.flush();

        String response = reader.readLine();

        // Parse response: 0|{{base64(result)}}
        String[] parts = ProtocolSecurity.splitChecksum(response);
        String[] components = parts[0].split("\\|", -1);
        String resultEncoded = components[1]; // This is {{base64(result)}}

        Assert.assertEquals("Hello, world!", converter.decodeParam(resultEncoded));

    }

    @Test
    public void testArgs() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);
        Converter converter = new DefaultConverter();
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // New secure format: 0|{{base64(meta)}}|{{base64(params)}}
        String meta = "cn.huiwings.tcprest.test.HelloWorldResource/sayHelloTo";
        String metaBase64 = ProtocolSecurity.encodeComponent(meta);

        // Encode parameter: "Jack!" -> {{base64}} -> base64 again
        String param = converter.encodeParam("Jack!");
        String paramsBase64 = ProtocolSecurity.encodeComponent(param);
        String request = "0|" + metaBase64 + "|" + paramsBase64;

        writer.println(request);
        writer.flush();

        String response = reader.readLine();

        // Parse response: 0|{{base64(result)}}
        String[] parts = ProtocolSecurity.splitChecksum(response);
        String[] components = parts[0].split("\\|", -1);
        String resultEncoded = components[1]; // This is {{base64(result)}}

        Assert.assertEquals("Hello, Jack!", converter.decodeParam(resultEncoded));

    }

    @Test
    public void testMultipleArgs() throws IOException {
        tcpRestServer.addResource(HelloWorldResource.class);
        Converter converter = new DefaultConverter();
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // New secure format: 0|{{base64(meta)}}|{{base64(params)}}
        String meta = "cn.huiwings.tcprest.test.HelloWorldResource/oneTwoThree";
        String metaBase64 = ProtocolSecurity.encodeComponent(meta);

        // Encode multiple parameters: {{p1}}:::{{p2}}:::{{p3}} -> base64
        String params = converter.encodeParam("One")
                + TcpRestProtocol.PARAM_SEPARATOR
                + converter.encodeParam("2")
                + TcpRestProtocol.PARAM_SEPARATOR
                + converter.encodeParam("true");
        String paramsBase64 = ProtocolSecurity.encodeComponent(params);
        String request = "0|" + metaBase64 + "|" + paramsBase64;

        writer.println(request);
        writer.flush();

        String response = reader.readLine();

        // Parse response: 0|{{base64(result)}}
        String[] parts = ProtocolSecurity.splitChecksum(response);
        String[] components = parts[0].split("\\|", -1);
        String resultEncoded = components[1]; // This is {{base64(result)}}

        Assert.assertEquals("One,2,true", converter.decodeParam(resultEncoded));

    }


}
