package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.Objects;

import static org.testng.Assert.*;

/**
 * E2E tests for server-side DTO/mapper validation when adding resources.
 *
 * <p>Verifies:</p>
 * <ul>
 *   <li>strictTypeCheck=true: addResource/addSingletonResource with unsupported DTO throws</li>
 *   <li>default: unsupported DTO only logs warning, server can start</li>
 *   <li>supported types (Serializable): no throw, client call succeeds</li>
 * </ul>
 */
public class ResourceTypeValidationE2ETest {

    private static final PortGenerator.PortRange portRange = PortGenerator.from(27000);

    private TcpRestServer server;
    private int port;

    @BeforeClass
    public void setup() throws Exception {
        port = portRange.next();
        server = new SingleThreadTcpRestServer(port);
        server.addResource(SupportedDtoServiceImpl.class);
        server.up();
        Thread.sleep(500);
    }

    @AfterClass
    public void tearDown() throws Exception {
        if (server != null) {
            server.down();
            server = null;
        }
        Thread.sleep(300);
    }

    // --- Unsupported: plain POJO, no Serializable, no mapper ---
    public static class PlainDto {
        private String value;

        public PlainDto() {}

        public PlainDto(String value) {
            this.value = value;
        }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PlainDto)) return false;
            PlainDto that = (PlainDto) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public interface UnsupportedDtoService {
        PlainDto echo(PlainDto in);
    }

    public static class UnsupportedDtoServiceImpl implements UnsupportedDtoService {
        @Override
        public PlainDto echo(PlainDto in) {
            return in == null ? null : new PlainDto(in.getValue());
        }
    }

    // --- Supported: Serializable DTO ---
    public static class SerializableDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String data;

        public SerializableDto() {}

        public SerializableDto(String data) {
            this.data = data;
        }

        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    public interface SupportedDtoService {
        SerializableDto echo(SerializableDto in);
    }

    public static class SupportedDtoServiceImpl implements SupportedDtoService {
        @Override
        public SerializableDto echo(SerializableDto in) {
            return in == null ? null : new SerializableDto(in.getData());
        }
    }

    @Test
    public void strictTypeCheck_addResource_unsupportedType_throws() throws Exception {
        int p = portRange.next();
        TcpRestServer s = new SingleThreadTcpRestServer(p);
        try {
            s.setStrictTypeCheck(true);
            s.addResource(UnsupportedDtoServiceImpl.class);
            fail("Expected IllegalStateException when adding resource with unsupported DTO and strictTypeCheck=true");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("PlainDto") || e.getMessage().contains("uses types"));
            assertTrue(e.getMessage().contains("Serializable") || e.getMessage().contains("mapper"));
        } finally {
            s.down();
        }
    }

    @Test
    public void strictTypeCheck_addSingletonResource_unsupportedType_throws() throws Exception {
        int p = portRange.next();
        TcpRestServer s = new SingleThreadTcpRestServer(p);
        try {
            s.setStrictTypeCheck(true);
            s.addSingletonResource(new UnsupportedDtoServiceImpl());
            fail("Expected IllegalStateException when adding singleton resource with unsupported DTO and strictTypeCheck=true");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("PlainDto") || e.getMessage().contains("uses types"));
        } finally {
            s.down();
        }
    }

    @Test
    public void default_addResource_unsupportedType_serverStarts() throws Exception {
        int p = portRange.next();
        TcpRestServer s = new SingleThreadTcpRestServer(p);
        try {
            s.addResource(UnsupportedDtoServiceImpl.class);
            s.up();
            Thread.sleep(500);
            assertEquals(s.getServerPort(), p);
        } finally {
            s.down();
        }
    }

    @Test
    public void supportedType_addResource_serverStartsAndCallSucceeds() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
            SupportedDtoService.class, "localhost", port
        );
        SupportedDtoService client = factory.getClient();
        SerializableDto input = new SerializableDto("e2e-data");
        SerializableDto result = client.echo(input);
        assertNotNull(result);
        assertEquals(result.getData(), "e2e-data");
    }

    @Test
    public void supportedType_addSingletonResource_serverStartsAndCallSucceeds() throws Exception {
        int p = portRange.next();
        TcpRestServer s = new SingleThreadTcpRestServer(p);
        try {
            s.addSingletonResource(new SupportedDtoServiceImpl());
            s.up();
            Thread.sleep(500);
            TcpRestClientFactory factory = new TcpRestClientFactory(
                SupportedDtoService.class, "localhost", p
            );
            SupportedDtoService client = factory.getClient();
            SerializableDto input = new SerializableDto("singleton-e2e");
            SerializableDto result = client.echo(input);
            assertNotNull(result);
            assertEquals(result.getData(), "singleton-e2e");
        } finally {
            s.down();
        }
    }
}
