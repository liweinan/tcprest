package cn.huiwings.tcprest.exception;

/**
 * Thrown when a security violation is detected in the TcpRest protocol.
 *
 * <p>Examples:
 * <ul>
 *   <li>Invalid checksum (message tampering detected)</li>
 *   <li>Malformed encoded components</li>
 *   <li>Class name not in whitelist</li>
 *   <li>Invalid class/method name format (injection attempt)</li>
 * </ul>
 *
 * @author Weinan Li
 * @date 2026-02-18
 */
public class SecurityException extends RuntimeException {

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
