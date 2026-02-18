package cn.huiwings.tcprest.invoker.v2;

import cn.huiwings.tcprest.exception.BusinessException;
import cn.huiwings.tcprest.exception.ProtocolException;
import cn.huiwings.tcprest.server.Context;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.testng.Assert.*;

/**
 * Tests for ProtocolV2Invoker.
 */
public class ProtocolV2InvokerTest {

    private ProtocolV2Invoker invoker;

    @BeforeClass
    public void setup() {
        invoker = new ProtocolV2Invoker();
    }

    // ========== Test Successful Invocations ==========

    @Test
    public void testInvoke_successfulCall() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("add", int.class, int.class);

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{5, 3});

        Object result = invoker.invoke(context);

        assertEquals(result, 8);
    }

    @Test
    public void testInvoke_stringReturn() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("echo", String.class);

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{"hello"});

        Object result = invoker.invoke(context);

        assertEquals(result, "hello");
    }

    @Test
    public void testInvoke_voidReturn() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("doNothing");

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{});

        Object result = invoker.invoke(context);

        assertNull(result);
    }

    @Test
    public void testInvoke_nullParameter() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("echo", String.class);

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{null});

        Object result = invoker.invoke(context);

        assertNull(result);
    }

    // ========== Test Business Exceptions ==========

    @Test(expectedExceptions = BusinessException.class)
    public void testInvoke_businessException() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("validateAge", int.class);

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{-1});

        invoker.invoke(context);
    }

    @Test
    public void testInvoke_businessException_message() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("validateAge", int.class);

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{-5});

        try {
            invoker.invoke(context);
            fail("Should have thrown BusinessException");
        } catch (BusinessException e) {
            assertTrue(e.getMessage().contains("Age must be non-negative"));
        }
    }

    @Test
    public void testInvoke_customBusinessException() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("validateEmail", String.class);

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{"invalid"});

        try {
            invoker.invoke(context);
            fail("Should have thrown ValidationException");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Invalid email format"));
            assertTrue(e instanceof BusinessException);
        }
    }

    // ========== Test Server Errors ==========

    @Test(expectedExceptions = NullPointerException.class)
    public void testInvoke_nullPointerException() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("causeNullPointer");

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{});

        invoker.invoke(context);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testInvoke_illegalStateException() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("causeIllegalState");

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{});

        invoker.invoke(context);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void testInvoke_arithmeticException() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("divide", int.class, int.class);

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{10, 0});

        invoker.invoke(context);
    }

    // ========== Test Protocol Exceptions ==========

    @Test(expectedExceptions = ProtocolException.class)
    public void testInvoke_nullContext() throws Exception {
        invoker.invoke(null);
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testInvoke_nullTargetInstance() throws Exception {
        Context context = new Context();
        context.setTargetInstance(null);
        context.setTargetMethod(TestService.class.getMethod("add", int.class, int.class));
        context.setParams(new Object[]{1, 2});

        invoker.invoke(context);
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testInvoke_nullTargetMethod() throws Exception {
        Context context = new Context();
        context.setTargetInstance(new TestService());
        context.setTargetMethod(null);
        context.setParams(new Object[]{1, 2});

        invoker.invoke(context);
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testInvoke_invalidArguments() throws Exception {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("add", int.class, int.class);

        Context context = new Context();
        context.setTargetInstance(service);
        context.setTargetMethod(method);
        context.setParams(new Object[]{"invalid", "types"}); // Wrong types

        invoker.invoke(context);
    }

    // ========== Test Instance Creation ==========

    @Test
    public void testCreateInstance_success() throws Exception {
        Object instance = invoker.createInstance(TestService.class);

        assertNotNull(instance);
        assertTrue(instance instanceof TestService);
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testCreateInstance_nullClass() throws Exception {
        invoker.createInstance(null);
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testCreateInstance_noDefaultConstructor() throws Exception {
        invoker.createInstance(NoDefaultConstructor.class);
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testCreateInstance_abstractClass() throws Exception {
        invoker.createInstance(AbstractService.class);
    }

    // ========== Test Helper Classes ==========

    /**
     * Test service with various method behaviors.
     */
    public static class TestService {
        public int add(int a, int b) {
            return a + b;
        }

        public String echo(String s) {
            return s;
        }

        public void doNothing() {
            // Does nothing
        }

        public void validateAge(int age) {
            if (age < 0) {
                throw new BusinessException("Age must be non-negative");
            }
        }

        public void validateEmail(String email) {
            if (!email.contains("@")) {
                throw new ValidationException("Invalid email format");
            }
        }

        public void causeNullPointer() {
            String s = null;
            s.length(); // NullPointerException
        }

        public void causeIllegalState() {
            throw new IllegalStateException("Invalid state");
        }

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

    /**
     * Class with no default constructor.
     */
    public static class NoDefaultConstructor {
        public NoDefaultConstructor(String arg) {
            // Requires argument
        }
    }

    /**
     * Abstract class for testing.
     */
    public static abstract class AbstractService {
        public abstract void doSomething();
    }
}
