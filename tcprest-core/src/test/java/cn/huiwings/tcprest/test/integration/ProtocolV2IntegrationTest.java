package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.DefaultTcpRestClient;
import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.exception.BusinessException;
import cn.huiwings.tcprest.exception.RemoteInvocationException;
import cn.huiwings.tcprest.protocol.ProtocolVersion;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * End-to-end integration tests for Protocol v2.
 *
 * <p>Tests the full request-response cycle with v2 protocol, including:</p>
 * <ul>
 *   <li>Method overloading support</li>
 *   <li>Exception propagation (business and server errors)</li>
 *   <li>Various data types (primitives, objects, arrays)</li>
 * </ul>
 */
public class ProtocolV2IntegrationTest {

    // Use dedicated port range for this test class (20000-20999)
    // Avoids conflicts with other tests and system services
    private static final int BASE_PORT = 20000;

    private TcpRestServer server;
    private int port;
    private OverloadedCalculator calculatorClient;
    private ExceptionTestService exceptionClient;

    @BeforeClass
    public void setup() throws Exception {
        port = BASE_PORT;

        // Start server in V2 mode with implementation classes
        server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.V2);

        // Register resource classes (can use addResource with implementation classes)
        server.addResource(OverloadedCalculatorImpl.class);
        server.addResource(ExceptionTestServiceImpl.class);

        server.up();

        Thread.sleep(500); // Wait for server startup

        // Create v2 clients
        TcpRestClientFactory calculatorFactory = new TcpRestClientFactory(
            OverloadedCalculator.class, "localhost", port
        );
        calculatorFactory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        calculatorClient = (OverloadedCalculator) calculatorFactory.getClient();

        TcpRestClientFactory exceptionFactory = new TcpRestClientFactory(
            ExceptionTestService.class, "localhost", port
        );
        exceptionFactory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        exceptionClient = (ExceptionTestService) exceptionFactory.getClient();
    }

    @AfterClass
    public void tearDown() throws Exception {
        if (server != null) {
            server.down();
            server = null;
        }
        Thread.sleep(2000); // Long delay to ensure port is fully released before next test class
    }

    // ========== Test Method Overloading ==========

    @Test
    public void testOverloading_intAdd() {
        int result = calculatorClient.add(5, 3);
        assertEquals(result, 8);
    }

    @Test
    public void testOverloading_doubleAdd() {
        double result = calculatorClient.add(2.5, 3.5);
        assertEquals(result, 6.0, 0.001);
    }

    @Test
    public void testOverloading_stringAdd() {
        String result = calculatorClient.add("Hello", "World");
        assertEquals(result, "HelloWorld");
    }

    @Test
    public void testOverloading_allThreeWork() {
        // Verify all three overloads work in sequence
        assertEquals(calculatorClient.add(10, 20), 30);
        assertEquals(calculatorClient.add(1.1, 2.2), 3.3, 0.001);
        assertEquals(calculatorClient.add("A", "B"), "AB");
    }

    @Test
    public void testOverloading_mixedParameters_stringInt() {
        String result = calculatorClient.process("test", 42);
        assertEquals(result, "process(String,int): test-42");
    }

    @Test
    public void testOverloading_mixedParameters_intString() {
        String result = calculatorClient.process(42, "test");
        assertEquals(result, "process(int,String): 42-test");
    }

    @Test
    public void testOverloading_singleParameter() {
        String result = calculatorClient.process("solo");
        assertEquals(result, "process(String): solo");
    }

    @Test
    public void testOverloading_noParameters() {
        int result = calculatorClient.getAnswer();
        assertEquals(result, 42);
    }

    @Test
    public void testOverloading_arrayParameter() {
        int result = calculatorClient.sum(new int[]{1, 2, 3, 4, 5});
        assertEquals(result, 15);
    }

    @Test
    public void testOverloading_multipleOverloadsInLoop() {
        for (int i = 0; i < 10; i++) {
            assertEquals(calculatorClient.add(i, i), i * 2);
            assertEquals(calculatorClient.add((double) i, (double) i), (double) (i * 2), 0.001);
        }
    }

    // ========== Test Exception Propagation ==========

    @Test
    public void testException_businessException_propagated() {
        try {
            exceptionClient.validateAge(-1);
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            // V2 should propagate exception with type info
            assertTrue(e.getMessage().contains("ValidationException") ||
                      e.getMessage().contains("Age must be non-negative"));
        }
    }

    @Test
    public void testException_businessException_withMessage() {
        try {
            exceptionClient.validateAge(-10);
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Age must be non-negative"));
        }
    }

    @Test
    public void testException_serverError_propagated() {
        try {
            exceptionClient.causeNullPointer();
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            // Should contain exception type info
            assertTrue(e.getMessage().contains("NullPointerException") ||
                      e.getMessage().contains("null"));
        }
    }

    @Test
    public void testException_arithmeticException() {
        try {
            exceptionClient.divide(10, 0);
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("ArithmeticException") ||
                      e.getMessage().contains("/ by zero"));
        }
    }

    @Test
    public void testException_validInput_noException() {
        String result = exceptionClient.validateAge(25);
        assertEquals(result, "Valid age: 25");
    }

    @Test
    public void testException_validDivision_noException() {
        int result = exceptionClient.divide(10, 2);
        assertEquals(result, 5);
    }

    // ========== Test Various Data Types ==========

    @Test
    public void testDataTypes_boolean() {
        boolean result = calculatorClient.isPositive(5);
        assertTrue(result);

        result = calculatorClient.isPositive(-3);
        assertFalse(result);
    }

    @Test
    public void testDataTypes_nullParameter() {
        String result = calculatorClient.echo(null);
        assertNull(result);
    }

    @Test
    public void testDataTypes_emptyString() {
        String result = calculatorClient.echo("");
        assertEquals(result, "");
    }

    @Test
    public void testDataTypes_largeNumbers() {
        long result = calculatorClient.multiplyLong(1000000000L, 2L);
        assertEquals(result, 2000000000L);
    }

    @Test
    public void testDataTypes_floatingPoint() {
        double result = calculatorClient.divide(10.0, 3.0);
        assertEquals(result, 3.333333333, 0.0001);
    }

    // ========== Test Service Interfaces ==========

    /**
     * Service interface with overloaded methods.
     */
    public interface OverloadedCalculator {
        // Basic overloading - same method name, different parameter types
        int add(int a, int b);
        double add(double a, double b);
        String add(String a, String b);

        // Mixed parameter types
        String process(String s, int i);
        String process(int i, String s);
        String process(String s);

        // No parameters
        int getAnswer();

        // Array parameters
        int sum(int[] numbers);

        // Utility methods
        boolean isPositive(int n);
        String echo(String s);
        long multiplyLong(long a, long b);
        double divide(double a, double b);
    }

    /**
     * Service interface for exception testing.
     */
    public interface ExceptionTestService {
        String validateAge(int age);
        void causeNullPointer();
        int divide(int a, int b);
    }

    // ========== Test Service Implementations ==========

    /**
     * Implementation with overloaded methods.
     */
    public static class OverloadedCalculatorImpl implements OverloadedCalculator {
        @Override
        public int add(int a, int b) {
            return a + b;
        }

        @Override
        public double add(double a, double b) {
            return a + b;
        }

        @Override
        public String add(String a, String b) {
            return a + b;
        }

        @Override
        public String process(String s, int i) {
            return "process(String,int): " + s + "-" + i;
        }

        @Override
        public String process(int i, String s) {
            return "process(int,String): " + i + "-" + s;
        }

        @Override
        public String process(String s) {
            return "process(String): " + s;
        }

        @Override
        public int getAnswer() {
            return 42;
        }

        @Override
        public int sum(int[] numbers) {
            int total = 0;
            for (int n : numbers) {
                total += n;
            }
            return total;
        }

        @Override
        public boolean isPositive(int n) {
            return n > 0;
        }

        @Override
        public String echo(String s) {
            return s;
        }

        @Override
        public long multiplyLong(long a, long b) {
            return a * b;
        }

        @Override
        public double divide(double a, double b) {
            return a / b;
        }
    }

    /**
     * Implementation that throws exceptions.
     */
    public static class ExceptionTestServiceImpl implements ExceptionTestService {
        @Override
        public String validateAge(int age) {
            if (age < 0) {
                throw new ValidationException("Age must be non-negative");
            }
            return "Valid age: " + age;
        }

        @Override
        public void causeNullPointer() {
            String s = null;
            s.length(); // NullPointerException
        }

        @Override
        public int divide(int a, int b) {
            return a / b; // Can throw ArithmeticException
        }
    }

    /**
     * Custom business exception for testing.
     */
    public static class ValidationException extends BusinessException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
