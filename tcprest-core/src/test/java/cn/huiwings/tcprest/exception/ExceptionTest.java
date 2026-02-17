package cn.huiwings.tcprest.exception;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests for Protocol v2 exception classes.
 */
public class ExceptionTest {

    // ========== ProtocolException Tests ==========

    @Test
    public void testProtocolException_message() {
        ProtocolException ex = new ProtocolException("Invalid format");

        assertEquals(ex.getMessage(), "Invalid format");
        assertNull(ex.getCause());
    }

    @Test
    public void testProtocolException_messageAndCause() {
        Exception cause = new IllegalArgumentException("Root cause");
        ProtocolException ex = new ProtocolException("Invalid format", cause);

        assertEquals(ex.getMessage(), "Invalid format");
        assertEquals(ex.getCause(), cause);
    }

    @Test
    public void testProtocolException_cause() {
        Exception cause = new IllegalArgumentException("Root cause");
        ProtocolException ex = new ProtocolException(cause);

        assertEquals(ex.getCause(), cause);
    }

    // ========== BusinessException Tests ==========

    @Test
    public void testBusinessException_noArgs() {
        BusinessException ex = new BusinessException();

        assertNull(ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void testBusinessException_message() {
        BusinessException ex = new BusinessException("Validation failed");

        assertEquals(ex.getMessage(), "Validation failed");
        assertNull(ex.getCause());
    }

    @Test
    public void testBusinessException_messageAndCause() {
        Exception cause = new IllegalArgumentException("Root cause");
        BusinessException ex = new BusinessException("Validation failed", cause);

        assertEquals(ex.getMessage(), "Validation failed");
        assertEquals(ex.getCause(), cause);
    }

    @Test
    public void testBusinessException_cause() {
        Exception cause = new IllegalArgumentException("Root cause");
        BusinessException ex = new BusinessException(cause);

        assertEquals(ex.getCause(), cause);
    }

    @Test
    public void testBusinessException_inheritance() {
        CustomBusinessException ex = new CustomBusinessException("Custom error");

        assertTrue(ex instanceof BusinessException);
        assertTrue(ex instanceof RuntimeException);
        assertEquals(ex.getMessage(), "Custom error");
    }

    // ========== RemoteInvocationException Tests ==========

    @Test
    public void testRemoteInvocationException_businessException() {
        RemoteInvocationException ex = new RemoteInvocationException(
            "ValidationException",
            "Invalid input"
        );

        assertTrue(ex.isBusinessException());
        assertFalse(ex.isServerError());
        assertEquals(ex.getRemoteExceptionType(), "ValidationException");
        assertTrue(ex.getMessage().contains("ValidationException"));
        assertTrue(ex.getMessage().contains("Invalid input"));
    }

    @Test
    public void testRemoteInvocationException_serverError() {
        RemoteInvocationException ex = new RemoteInvocationException(
            "NullPointerException",
            "Object is null",
            false
        );

        assertFalse(ex.isBusinessException());
        assertTrue(ex.isServerError());
        assertEquals(ex.getRemoteExceptionType(), "NullPointerException");
        assertTrue(ex.getMessage().contains("NullPointerException"));
        assertTrue(ex.getMessage().contains("Object is null"));
    }

    @Test
    public void testRemoteInvocationException_withCause() {
        Exception cause = new IllegalStateException("Internal error");
        RemoteInvocationException ex = new RemoteInvocationException(
            "ServerException",
            "Server failed",
            false,
            cause
        );

        assertEquals(ex.getCause(), cause);
        assertEquals(ex.getRemoteExceptionType(), "ServerException");
        assertTrue(ex.isServerError());
    }

    @Test
    public void testRemoteInvocationException_nullType() {
        RemoteInvocationException ex = new RemoteInvocationException(
            null,
            "Some error"
        );

        assertNull(ex.getRemoteExceptionType());
        assertEquals(ex.getMessage(), "Some error");
    }

    @Test
    public void testRemoteInvocationException_emptyType() {
        RemoteInvocationException ex = new RemoteInvocationException(
            "",
            "Some error"
        );

        assertEquals(ex.getRemoteExceptionType(), "");
        assertEquals(ex.getMessage(), "Some error");
    }

    @Test
    public void testRemoteInvocationException_nullMessage() {
        RemoteInvocationException ex = new RemoteInvocationException(
            "TestException",
            null
        );

        assertEquals(ex.getRemoteExceptionType(), "TestException");
        assertTrue(ex.getMessage().contains("TestException"));
    }

    @Test
    public void testRemoteInvocationException_nullTypeAndMessage() {
        RemoteInvocationException ex = new RemoteInvocationException(
            null,
            null
        );

        assertNull(ex.getRemoteExceptionType());
        assertEquals(ex.getMessage(), "Remote invocation failed");
    }

    @Test
    public void testRemoteInvocationException_formatting() {
        RemoteInvocationException ex1 = new RemoteInvocationException("Type1", "Message1");
        assertEquals(ex1.getMessage(), "Type1: Message1");

        RemoteInvocationException ex2 = new RemoteInvocationException("Type2", "");
        assertEquals(ex2.getMessage(), "Type2: ");

        RemoteInvocationException ex3 = new RemoteInvocationException("", "Message3");
        assertEquals(ex3.getMessage(), "Message3");
    }

    // ========== Test Helper Classes ==========

    /**
     * Custom business exception for inheritance testing.
     */
    public static class CustomBusinessException extends BusinessException {
        public CustomBusinessException(String message) {
            super(message);
        }
    }
}
