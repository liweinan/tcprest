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

    // ========== RemoteBusinessException Tests ==========

    @Test
    public void testRemoteBusinessException_basic() {
        RemoteBusinessException ex = new RemoteBusinessException(
            "com.example.OrderValidationException",
            "Order amount exceeds limit"
        );

        assertTrue(ex.isBusinessException());
        assertFalse(ex.isServerError());
        assertEquals(ex.getRemoteExceptionType(), "com.example.OrderValidationException");
        assertTrue(ex.getMessage().contains("OrderValidationException"));
        assertTrue(ex.getMessage().contains("Order amount exceeds limit"));
    }

    @Test
    public void testRemoteBusinessException_withCause() {
        Exception cause = new IllegalArgumentException("Invalid parameter");
        RemoteBusinessException ex = new RemoteBusinessException(
            "com.example.ValidationException",
            "Validation failed",
            cause
        );

        assertEquals(ex.getCause(), cause);
        assertEquals(ex.getRemoteExceptionType(), "com.example.ValidationException");
        assertTrue(ex.isBusinessException());
    }

    // ========== RemoteServerException Tests ==========

    @Test
    public void testRemoteServerException_basic() {
        RemoteServerException ex = new RemoteServerException(
            "com.example.CustomDatabaseException",
            "Connection pool exhausted"
        );

        assertFalse(ex.isBusinessException());
        assertTrue(ex.isServerError());
        assertEquals(ex.getRemoteExceptionType(), "com.example.CustomDatabaseException");
        assertTrue(ex.getMessage().contains("CustomDatabaseException"));
        assertTrue(ex.getMessage().contains("Connection pool exhausted"));
    }

    @Test
    public void testRemoteServerException_withCause() {
        Exception cause = new RuntimeException("Internal error");
        RemoteServerException ex = new RemoteServerException(
            "com.example.ServerException",
            "Server failed",
            cause
        );

        assertEquals(ex.getCause(), cause);
        assertEquals(ex.getRemoteExceptionType(), "com.example.ServerException");
        assertTrue(ex.isServerError());
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
