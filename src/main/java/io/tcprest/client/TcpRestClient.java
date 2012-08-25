package io.tcprest.client;

import java.io.IOException;

/**
 * @author Weinan Li
 * @date 07 30 2012
 */
public interface TcpRestClient {

    /**
     * The sendReqeust method will create a client socket and send the processed messages to server,
     * and get the response from server and return it.
     *
     * @param request The processed incoming request from client
     * @return The un-processed response from server
     * @throws IOException
     */
    public String sendRequest(String request, int timeout) throws Exception;

    public String getDeletgatedClassName();
}
