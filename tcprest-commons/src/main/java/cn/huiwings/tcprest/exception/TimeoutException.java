package cn.huiwings.tcprest.exception;

/**
 * Unchecked timeout exception for TcpRest client operations.
 *
 * <p>This exception is thrown when a client-side timeout occurs during
 * a remote method invocation. It wraps the underlying {@link java.net.SocketTimeoutException}
 * as an unchecked exception to avoid dynamic proxy wrapping issues.</p>
 *
 * @author Weinan Li
 * @created_at 08 21 2012
 */
public class TimeoutException extends RuntimeException {

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public TimeoutException(Throwable cause) {
        super(cause);
    }
}
