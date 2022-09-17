/*
 * Copyright (c) 2018-2022, Rapidomize.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * OR contact:
 * contact@rapidomize.com
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.rapidomize.ics.sdk;

import com.rapidomize.ics.sdk.common.Json;
import com.rapidomize.ics.sdk.common.Status;

/**
 * Handler to be implemented by the client (mobile/server/device apps)
 * to handle read/write/execute/ack requests from the server/cloud.
 * read/write/execute operations are performed on a given attributes/resource.
 *
 * N.B. this is an async handler, so implementation must not block the callback methods!
 *
 * Server/Cloud request has following format:
 *
 *  {"n":'attribute name", "op":op-code}
 *
 *  where:
 *     attribute name - is the attribute/resourceName used when defining ICApp payload or App registration time.
 *     op - operation, this has following integer value for each operations
 *          READ=1, WRITE=2, UPDATE=3, DELETE=4, EXEC=5
 *          @see com.rapidomize.ics.sdk.events.Message
 *
 *      write, update, delete operations are done on the same method write() with 'op' having above values.
 *      So implementations can handle the behavior differently as required in the method {@code LifeCycleHandler.write()}
 *
 *
 */
public interface LifeCycleHandler extends ResponseHandler{

    /**
     * For Websocket and Mqtt subscription clients, sdk will notify the client's implementation once the app/device
     * client establish a connection with the server/cloud platform. Platform will keep the session alive to further
     * communicate with the client
     */
    default void connected(){}

    /**
     * Request from Server/Cloud to read config attributes from App/Device. Multiple attributes
     *
     *  {"n":"attribute-name", "op": op-code}
     *
     *  where op-code is 0x01 for read operations
     *  @see com.rapidomize.ics.sdk.events.Message for operation request codes
     *
     * Response would use the format
     *
     *  {"n":"attribute-name", "v": attribute-value}
     *
     *  if response has multiple entries for the requested attributes, responses should be sent in a
     *  json object array.
     *
    *   [{"n":"attribute-name", "v": attribute-value},
     *  {"n":"attribute-name", "v": attribute-value},
     *  {...}]
     *
     *  mix response are not supported.
     *
     * @param reqest JSON containing attributes names
     * @return json JSON containing attributes names/values
     * @throws Exception in case of exception
     */
    default Json read(Json reqest) throws Exception { throw new IllegalStateException("Not Implemented");}

    /**
     * Request from Server/Cloud to write, update or delete a given attributes of the client managed by the platform
     *
     * write with single item would use:
     *
     *  {"n":"attribute-name", "v": attribute-value, "op":op-code}
     *
     *  where op-code is one of 0x2, 0x03, 0x04 for write, update and delete respectively
     *  @see com.rapidomize.ics.sdk.events.Message for operation request codes
     *
     * for multiple values each json object must be in a json array.
     *
     * [{"n":"attribute-name", "v": attribute-value, "op":op-code},
     *  {"n":"attribute-name", "v": attribute-value, "op":op-code},
     *  {...}]
     *
     * @param reqest JSON containing attributes names
     * @return status SUCCESS or FAILURE
     * @throws Exception in case of exception
     */
    default Status write(Json reqest) throws Exception { throw new IllegalStateException("Not Implemented");}

    /**
     * Execute/trigger an operation designated by a named attributes on the client managed by the Server/Cloud platform.
     * e.g. actuator of a device or method/function/module or service ...etc of a software entity
     *
     * {"n":"attribute-name", "v": attribute-value,  "op":op-code}
     *
     * where op-code is 0x05 for execute operations
     * @see com.rapidomize.ics.sdk.events.Message for operation request codes
     *
     * e.g.
     * 1. value v could be on/off or object/binary value , and is based on the resources/attributes/ to execute the
     *    operation on.
     * 2. could be parameter value for a method/function/module or service invocation
     *
     *
     * @param reqest JSON containing attributes names
     * @return status SUCCESS or FAILURE
     * @throws Exception in case of exception
     */
    default Status exec(Json reqest) throws Exception { throw new IllegalStateException("Not Implemented");}

}
