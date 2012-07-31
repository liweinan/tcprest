package io.tcprest.exception;

/**
 * Throws when extractor cannot parse the request.
 *
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class ParseException extends Exception {
    public ParseException() {
        super();
    }

    public ParseException(String message) {
        super(message);
    }

}
