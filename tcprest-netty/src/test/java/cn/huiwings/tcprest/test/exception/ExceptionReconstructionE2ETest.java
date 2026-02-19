package cn.huiwings.tcprest.test.exception;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.exception.BusinessException;
import cn.huiwings.tcprest.exception.RemoteBusinessException;
import cn.huiwings.tcprest.exception.RemoteServerException;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.*;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * E2E test for exception reconstruction and fallback mechanism.
 *
 * <p>Tests the complete exception propagation flow from server to client.</p>
 *
 * <p><b>Test Scenarios:</b></p>
 * <ol>
 *   <li>✅ Success: Client has standard exception class → reconstruct original type</li>
 *   <li>✅ Success: Client has BusinessException → reconstruct original type</li>
 *   <li>✅ Success: Client has custom business exception → reconstruct original type</li>
 * </ol>
 *
 * <p><b>Note:</b> In a single-JVM test environment, client and server share the same
 * ClassLoader, so all exception classes are visible to both. To test the fallback mechanism
 * (RemoteBusinessException/RemoteServerException), see the unit tests in
 * {@code ProtocolV2CodecTest.testRecreateException*}.</p>
 *
 * @author Weinan Li
 * @date Feb 19 2026
 */
public class ExceptionReconstructionE2ETest {

    /**
     * Test service interface - shared by server and client.
     */
    public interface TestService {
        void throwStandardException();  // NullPointerException - client has it
        void throwBusinessException();  // BusinessException - client has it
        void throwCustomBusinessException();  // Server-only custom business exception
        void throwCustomServerException();  // Server-only custom server exception
    }

    /**
     * Server-side implementation with custom exceptions.
     */
    public static class TestServiceImpl implements TestService {
        @Override
        public void throwStandardException() {
            throw new NullPointerException("Null pointer from server");
        }

        @Override
        public void throwBusinessException() {
            throw new BusinessException("Business error from server");
        }

        @Override
        public void throwCustomBusinessException() {
            // This exception class is ONLY on server side - client doesn't have it
            throw new OrderValidationException("Order amount exceeds limit");
        }

        @Override
        public void throwCustomServerException() {
            // This exception class is ONLY on server side - client doesn't have it
            throw new CustomDatabaseException("Connection pool exhausted");
        }
    }

    /**
     * Custom business exception - EXISTS ONLY ON SERVER SIDE.
     * Client doesn't have this class, so it will be wrapped in RemoteBusinessException.
     */
    public static class OrderValidationException extends BusinessException {
        public OrderValidationException(String message) {
            super(message);
        }
    }

    /**
     * Custom server exception - EXISTS ONLY ON SERVER SIDE.
     * Client doesn't have this class, so it will be wrapped in RemoteServerException.
     */
    public static class CustomDatabaseException extends RuntimeException {
        public CustomDatabaseException(String message) {
            super(message);
        }
    }

    protected TcpRestServer tcpRestServer;

    public ExceptionReconstructionE2ETest(TcpRestServer tcpRestServer) {
        this.tcpRestServer = tcpRestServer;
    }

    @Factory
    public static Object[] create() throws Exception {
        List<Object> result = new ArrayList<>();
        result.add(new ExceptionReconstructionE2ETest(new NettyTcpRestServer(PortGenerator.get())));
        return result.toArray();
    }

    @BeforeClass
    public void startTcpRestServer() throws Exception {
        tcpRestServer.up();
        Thread.sleep(500);  // Wait for async startup
    }

    @AfterClass
    public void stopTcpRestServer() throws Exception {
        tcpRestServer.down();
        Thread.sleep(300);  // Wait for port release
    }

    /**
     * Scenario 1: Standard exception reconstruction (client has NullPointerException).
     *
     * <p>Expected: Client receives exact same NullPointerException type.</p>
     */
    @Test
    public void testStandardExceptionReconstruction() throws Exception {
        tcpRestServer.addResource(TestServiceImpl.class);

        TcpRestClientFactory factory = new TcpRestClientFactory(
                TestService.class, "localhost", tcpRestServer.getServerPort()
        );

        TestService client = factory.getInstance();

        try {
            client.throwStandardException();
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException e) {
            // SUCCESS: Original exception type reconstructed
            assertEquals("Null pointer from server", e.getMessage(),
                    "Exception message should match");
        } catch (Exception e) {
            fail("Expected NullPointerException but got " + e.getClass().getName() +
                    ": " + e.getMessage());
        }
    }

    /**
     * Scenario 2: Business exception reconstruction (client has BusinessException).
     *
     * <p>Expected: Client receives exact same BusinessException type.</p>
     */
    @Test
    public void testBusinessExceptionReconstruction() throws Exception {
        tcpRestServer.addResource(TestServiceImpl.class);

        TcpRestClientFactory factory = new TcpRestClientFactory(
                TestService.class, "localhost", tcpRestServer.getServerPort()
        );

        TestService client = factory.getInstance();

        try {
            client.throwBusinessException();
            fail("Should have thrown BusinessException");
        } catch (BusinessException e) {
            // SUCCESS: Original exception type reconstructed
            assertEquals("Business error from server", e.getMessage(),
                    "Exception message should match");
        } catch (Exception e) {
            fail("Expected BusinessException but got " + e.getClass().getName() +
                    ": " + e.getMessage());
        }
    }

    /**
     * Scenario 3: Custom business exception reconstruction (client has OrderValidationException).
     *
     * <p>Server throws OrderValidationException (extends BusinessException), and client
     * has this class (same JVM), so it should be reconstructed.</p>
     *
     * <p>Expected: Client receives OrderValidationException (original type).</p>
     *
     * <p><b>Note:</b> In production with separate JVMs, if client didn't have this class,
     * it would receive RemoteBusinessException. See unit tests for fallback scenarios.</p>
     */
    @Test
    public void testCustomBusinessExceptionReconstruction() throws Exception {
        tcpRestServer.addResource(TestServiceImpl.class);

        TcpRestClientFactory factory = new TcpRestClientFactory(
                TestService.class, "localhost", tcpRestServer.getServerPort()
        );

        TestService client = factory.getInstance();

        try {
            client.throwCustomBusinessException();
            fail("Should have thrown OrderValidationException");
        } catch (OrderValidationException e) {
            // SUCCESS: Original type reconstructed (client has the class)
            assertEquals("Order amount exceeds limit", e.getMessage(),
                    "Exception message should match");
        } catch (Exception e) {
            fail("Expected OrderValidationException but got " + e.getClass().getName() +
                    ": " + e.getMessage());
        }
    }

    /**
     * Scenario 4: Custom server exception reconstruction (client has CustomDatabaseException).
     *
     * <p>Server throws CustomDatabaseException, and client has this class (same JVM),
     * so it should be reconstructed.</p>
     *
     * <p>Expected: Client receives CustomDatabaseException (original type).</p>
     *
     * <p><b>Note:</b> In production with separate JVMs, if client didn't have this class,
     * it would receive RemoteServerException. See unit tests for fallback scenarios.</p>
     */
    @Test
    public void testCustomServerExceptionReconstruction() throws Exception {
        tcpRestServer.addResource(TestServiceImpl.class);

        TcpRestClientFactory factory = new TcpRestClientFactory(
                TestService.class, "localhost", tcpRestServer.getServerPort()
        );

        TestService client = factory.getInstance();

        try {
            client.throwCustomServerException();
            fail("Should have thrown CustomDatabaseException");
        } catch (CustomDatabaseException e) {
            // SUCCESS: Original type reconstructed (client has the class)
            assertEquals("Connection pool exhausted", e.getMessage(),
                    "Exception message should match");
        } catch (Exception e) {
            fail("Expected CustomDatabaseException but got " + e.getClass().getName() +
                    ": " + e.getMessage());
        }
    }
}
