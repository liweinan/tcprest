package cn.huiwings.tcprest.test.exception;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.exception.BusinessException;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.*;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Tests that verify server-side exceptions are properly propagated to the client
 * with their original exception types intact.
 *
 * <p>This test validates the Protocol V2 exception reconstruction mechanism
 * implemented in {@link cn.huiwings.tcprest.codec.v2.ProtocolV2Codec#recreateException}.</p>
 *
 * @author Weinan Li
 * @date Feb 19 2026
 */
public class ExceptionPropagationTest {

    /**
     * Test service interface with methods that throw different exception types.
     */
    public interface ExceptionTestService {
        void throwNullPointer();
        void throwIllegalArgument();
        void throwCustomException();
    }

    /**
     * Test service implementation that throws various exceptions.
     */
    public static class ExceptionTestServiceImpl implements ExceptionTestService {
        @Override
        public void throwNullPointer() {
            throw new NullPointerException("Null pointer from server");
        }

        @Override
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid argument from server");
        }

        @Override
        public void throwCustomException() {
            throw new BusinessException("Custom business error");
        }
    }

    protected TcpRestServer tcpRestServer;

    public ExceptionPropagationTest(TcpRestServer tcpRestServer) {
        this.tcpRestServer = tcpRestServer;
    }

    @Factory
    public static Object[] create() throws Exception {
        List<Object> result = new ArrayList<>();
        result.add(new ExceptionPropagationTest(new NettyTcpRestServer(PortGenerator.get())));
        return result.toArray();
    }

    @BeforeClass
    public void startTcpRestServer() throws Exception {
        tcpRestServer.up();
        // Delay to ensure async server is fully started
        Thread.sleep(500);
    }

    @AfterClass
    public void stopTcpRestServer() throws Exception {
        tcpRestServer.down();
        // Wait for port to be fully released
        Thread.sleep(300);
    }

    /**
     * Test 1: NullPointerException should propagate with correct type and message.
     */
    @Test
    public void testNullPointerExceptionPropagation() throws Exception {
        tcpRestServer.addResource(ExceptionTestServiceImpl.class);

        TcpRestClientFactory factory = new TcpRestClientFactory(
                ExceptionTestService.class, "localhost", tcpRestServer.getServerPort()
        );

        ExceptionTestService client = factory.getInstance();

        try {
            client.throwNullPointer();
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            // SUCCESS: Exception type propagated correctly
            assertEquals("Null pointer from server", e.getMessage(),
                    "Exception message should match");
        } catch (Exception e) {
            fail("Expected NullPointerException but got " + e.getClass().getName() +
                    ": " + e.getMessage());
        }
    }

    /**
     * Test 2: IllegalArgumentException should propagate with correct type and message.
     */
    @Test
    public void testIllegalArgumentExceptionPropagation() throws Exception {
        tcpRestServer.addResource(ExceptionTestServiceImpl.class);

        TcpRestClientFactory factory = new TcpRestClientFactory(
                ExceptionTestService.class, "localhost", tcpRestServer.getServerPort()
        );

        ExceptionTestService client = factory.getInstance();

        try {
            client.throwIllegalArgument();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // SUCCESS: Exception type propagated correctly
            assertEquals("Invalid argument from server", e.getMessage(),
                    "Exception message should match");
        } catch (Exception e) {
            fail("Expected IllegalArgumentException but got " + e.getClass().getName() +
                    ": " + e.getMessage());
        }
    }

    /**
     * Test 3: Custom BusinessException should propagate with correct type and message.
     */
    @Test
    public void testBusinessExceptionPropagation() throws Exception {
        tcpRestServer.addResource(ExceptionTestServiceImpl.class);

        TcpRestClientFactory factory = new TcpRestClientFactory(
                ExceptionTestService.class, "localhost", tcpRestServer.getServerPort()
        );

        ExceptionTestService client = factory.getInstance();

        try {
            client.throwCustomException();
            fail("Should have thrown BusinessException");
        } catch (BusinessException e) {
            // SUCCESS: Exception type propagated correctly
            assertEquals("Custom business error", e.getMessage(),
                    "Exception message should match");
        } catch (Exception e) {
            fail("Expected BusinessException but got " + e.getClass().getName() +
                    ": " + e.getMessage());
        }
    }
}
