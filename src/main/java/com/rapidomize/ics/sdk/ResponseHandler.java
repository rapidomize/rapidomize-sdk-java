package com.rapidomize.ics.sdk;

import com.rapidomize.ics.sdk.common.Json;

/**
 */
public interface ResponseHandler {

    /**
     * Client need to implement this to handle responses from the Server/Cloud platform as ACK (on success)
     * or error state with respective payload.
     *
     * where op-code contain status
     * @see com.rapidomize.ics.sdk.events.Message for response status codes
     *
     * @param response payload from the server
     * @throws Exception in case of exception
     */
    void ack(Json response) throws Exception;

    /**
     *  When {@code Client.await()} is called on Client, this method should return true if the client instance need to be
     *  shutdown gracefully.
     *
     * @return true if the shutdown is required
     */
    default boolean shutdown() {return false;}


    /**
     * Client need to implement this to handle exceptions reported by the Server/Cloud platform.
     *
     * @param e in case of exception
     */
    void onException(Exception e);
}
