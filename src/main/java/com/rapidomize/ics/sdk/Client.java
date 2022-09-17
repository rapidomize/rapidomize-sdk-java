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

import com.rapidomize.ics.sdk.common.Conf;
import com.rapidomize.ics.sdk.common.Json;
import com.rapidomize.ics.sdk.common.State;
import com.rapidomize.ics.sdk.common.Vld;
import com.rapidomize.ics.sdk.events.Message;
import com.rapidomize.ics.sdk.events.MessageHandler;
import com.rapidomize.ics.sdk.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;

import static com.rapidomize.ics.sdk.events.Message.TYPE.*;


/**
 * Asynchronous client for the Rapidomize server/cloud platform.
 *
 * This allows:
 * - Calling REST API,
 * - Executing ICApps remotely using HTTPS (REST), Websocket and MQTT
 * - Capturing data for analytics
 *
 */
public class Client {
    private final Logger logger = LoggerFactory.getLogger(Client.class);
    static final Conf CONF = Conf.getInstance();
    static final int DEFAULT_TRANSPORT= CONF.getIntProperty("transport.type");

    int transport;

    protected MessageHandler msghandler;

    protected Client() {
    }

    /**
     * Create a App/Device client with default transport HTTPS to remotely interact with the Rapidomize
     * server/cloud platform. Clients who ONLY send data/events to the platform without needing to respond to platform
     * sent requests, should use this constructor.
     *
     * Clients who need responding to platform sent requests (via ICApps settings) e.g. for IoT devices or monitoring Apps,
     * need to connect using websocket/mqtt so must use the constructor {@link Client#Client(String, String, int)}
     * or should set the {@code transport.type} in the {@code client.properties} to the one of websocket/mqtt
     *
     * @param appId  App/Device ID
     * @param token  App/Device Token
     * @throws Exception in case of exception in case of exception
     */
    public Client(String appId, String token) throws Exception {
        this(appId, token, DEFAULT_TRANSPORT);
    }

    /**
     * Create an App/Device client to remotely interact with the Rapidomize server/cloud platform.
     *
     * Clients who need responding to platform sent requests (via ICApps settings) e.g. for IoT devices or monitoring Apps,
     * need to connect using websocket/mqtt (WSS=1 and MQTT=2).
     *
     * @param appId  App/Device ID
     * @param token  App/Device Token
     * @param transport  Protocol to be used for {@link Transport}. Possible values HTTPS=0, WSS=1 and MQTT=2.
     * @throws Exception in case of exception
     */
    public Client(String appId, String token, int transport) throws Exception {
        Vld.checkEmpty(appId, "appId");
        Vld.checkEmpty(token, "token");
        msghandler = new MessageHandler(appId, token, transport);
        this.transport = transport;
    }

    /**
     * Trigger an ICApp with input JSON data as string.
     *
     * @param icappId ICApp ID
     * @param data JSON encoded string as ICApp input data
     * @throws Exception in case of exception
     */
    public void trigger(String icappId, String data) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(data, "data");
        msghandler.setHandler(null);
        msghandler.outbound(Message.WRITE, new Message(icappId, Json.fromJson(data)));
    }

    /**
     * Trigger an ICApp with input JSON data as string.
     *
     * @param icappId ICApp ID
     * @param data JSON encoded string as ICApp input data
     * @param rh callback handler instance of {@link ResponseHandler}
     * @throws Exception in case of exception
     */
    public void trigger(String icappId, String data, ResponseHandler rh) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(data, "data");
        Vld.checkNull(rh, "ResponseHandler");
        msghandler.setHandler(rh);
        msghandler.outbound(Message.WRITE, new Message(icappId, Json.fromJson(data)));
    }

    /**
     * Trigger an ICApp with Base64 encoded binary data. The server/cloud platform does not interpret or decode these
     * data, so if you are not intending to process these data using ICApps, you can also send end-to-end encrypted
     * binary data thi way. As ICApp works using JSON and macros, on the server/cloud platform, wehn configuring ICApps
     * you will use the key "payload" i.e. {"payload": base64 encoded data}
     *
     * @param icappId  ICApp ID
     * @param data Base64 encoded binary data as ICApp input data
     * @throws Exception in case of exception
     */
    public void trigger(String icappId, byte [] data) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkNull(data, "data");
        msghandler.setHandler(null);
        msghandler.outbound(Message.WRITE, new Message(icappId, new Json("payload", data)));
    }

    /**
     * Trigger an ICApp with Base64 encoded binary data. The server/cloud platform does not interpret or decode these
     * data, so if you are not intending to process these data using ICApps, you can also send end-to-end encrypted
     * binary data this way. As ICApp works using JSON and macros, on the server/cloud platform, when configuring ICApps
     * you will use the key "payload"  i.e. {"payload": base64 encoded data}
     *
     * @param icappId  ICApp ID
     * @param data Base64 encoded binary data as ICApp input data
     * @param rh callback rh instance of {@link ResponseHandler}
     * @throws Exception in case of exception
     */
    public void trigger(String icappId, byte [] data, ResponseHandler rh) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkNull(data, "data");
        Vld.checkNull(rh, "ResponseHandler");
        msghandler.setHandler(rh);
        msghandler.outbound(Message.WRITE, new Message(icappId, new Json("payload", data)));
    }

    /**
     * Trigger an ICApp with input data
     *
     * @param icappId ICApp ID
     * @param data JSON as ICApp input data
     * @throws Exception in case of exception
     */
    public void trigger(String icappId, Json data) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(data, "data");
        msghandler.setHandler(null);
        msghandler.outbound(Message.WRITE, new Message(icappId, data));
    }

    /**
     * Trigger an ICApp with input data
     *
     * @param icappId ICApp ID
     * @param data JSON as ICApp input data
     * @param rh callback rh instance of {@link ResponseHandler}
     * @throws Exception in case of exception
     */
    public void trigger(String icappId, Json data, ResponseHandler rh) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(data, "data");
        Vld.checkNull(rh, "ResponseHandler");
        msghandler.setHandler(rh);
        msghandler.outbound(Message.WRITE, new Message(icappId, data));
    }

    /**
     * Trigger an ICApp with an Event.  Compared to regular ICApp data, Events are used for Analytics.
     *
     * @param icappId ICApp ID
     * @param event as JSON string
     * @throws Exception in case of exception
     */
    public void trigger_ev(String icappId, String event) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(event, "event");
        msghandler.setHandler(null);
        msghandler.outbound(Message.WRITE,  new Message(EVENT, icappId, Json.fromJson(event)));
    }

    /**
     * Trigger an ICApp with an Event. Events are used for ETL/ELT or analytics.
     *
     * @param icappId ICApp ID
     * @param event as JSON
     * @throws Exception in case of exception
     */
    public void trigger_ev(String icappId, Json event) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(event, "event");
        msghandler.setHandler(null);
        msghandler.outbound(Message.WRITE,  new Message(EVENT, icappId, event));
    }

    /**
     * Trigger an ICApp with an Event. Events are used for ETL/ELT or analytics.
     *
     * @param icappId ICApp ID
     * @param event as JSON
     * @throws Exception in case of exception
     */
    public void trigger_ev(String icappId, Json event, ResponseHandler rh) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(event, "event");
        Vld.checkNull(rh, "ResponseHandler");
        msghandler.setHandler(rh);
        msghandler.outbound(Message.WRITE,  new Message(EVENT, icappId, event));
    }

    /**
     * Trigger an ICApp with a file and an event as input. This allows uploading small file < 5mb and
     * triggering a ICApp with the file & the event data
     *
     * @param icappId ICApp ID
     * @param file  files to be uploaded
     * @param fileEvent a JSON with event data and a mandatory attribute 'file' to represent the file name
     *                  & optionally media type of the file
     * @throws Exception in case of exception
     */
    public void upload(String icappId, File file, Json fileEvent, ResponseHandler rh) throws Exception {
        Vld.checkNull(file, "file");
        Vld.checkEmpty(fileEvent, "fileEvent");

        String fn = fileEvent.getString("file");
        if( fn == null || !fn.equals(file.getName()))
            throw new IllegalArgumentException("fileEven must contain 'file' as attribute matching the file name!");

        try (
            InputStream in = new FileInputStream(file);
        ) {
            upload(icappId, in, fileEvent, rh);
        }
    }

    /**
     * Trigger an ICApp with a file and an event as input. This allows uploading small file < 5mb and
     * triggering a ICApp with the file & the event data
     *
     * @param icappId ICApp ID
     * @param in  files (as stream) to be uploaded
     * @param fileEvent a JSON with event data and a mandatory attribute 'file' to represent the file name
     *                  & optionally media type of the file
     * @throws Exception in case of exception
     */
    public void upload(String icappId, InputStream in, Json fileEvent, ResponseHandler rh) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkNull(in, "file");
        Vld.checkEmpty(fileEvent, "fileEvent");

        String fn = fileEvent.getString("file");
        if( fn == null)
            throw new IllegalArgumentException("fileEven must contain 'file' as attribute matching the file name!");
        Vld.checkNull(rh, "ResponseHandler");
        msghandler.setHandler(rh);

        //for now, we have to read the whole file into memory as we need to send the file at once without chunking.
        byte[] buf = new byte[in.available()];
        in.read(buf);

        //all binary data must be base64 encoded
        String fileData = Base64.getEncoder().encodeToString(buf);

        Json data = new Json("_fd", fileData, "_data", fileEvent);

        msghandler.outbound(Message.WRITE,  new Message(BIN, icappId, data));
    }


    /**
     * Convenient method to invoke user defined GET REST API
     *
     * @param icappId ICApp ID
     * @param path API endpoint path
     * @param rh callback rh see above
     */
    public void get(String icappId, String path, ResponseHandler rh) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(path, "path");
        Vld.checkNull(rh, "ResponseHandler");
        mustbeHttps();

        msghandler.setHandler(rh);
        msghandler.outbound(Message.READ, new Message(API, _gwpath(path, icappId), (Json)null));
    }

    /**
     * Convenient method to invoke user defined POST REST API
     *
     * @param icappId ICApp ID
     * @param path API endpoint path
     * @param data inbound data for the ICApp, is either a object or an array of objects.  For bulk operations you can
     *             send data as an array of objects.ICApp will be triggered for each object in the array.
     * @param rh callback rh see above
     */
    public void post(String icappId, String path, Json data,  ResponseHandler rh) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(path, "path");
        Vld.checkEmpty(data, "data");
        Vld.checkNull(rh, "ResponseHandler");
        mustbeHttps();

        msghandler.setHandler(rh);
        msghandler.outbound(Message.WRITE, new Message(API, _gwpath(path, icappId), data));
    }

    /**
     * Convenient method to invoke user defined POST REST API
     *
     * @param icappId ICApp ID
     * @param path API endpoint path
     * @param data inbound data for the ICApp, is either a object or an array of objects.  For bulk operations you can
     *             send data as an array of objects.ICApp will be triggered for each object in the array.
     */
    public void post(String icappId, String path, Json data) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(path, "path");
        Vld.checkEmpty(data, "data");
        mustbeHttps();
        msghandler.setHandler(null);
        msghandler.outbound(Message.WRITE, new Message(API, _gwpath(path, icappId), data));
    }

    /**
     * Convenient method to invoke user defined PUT REST API
     *
     * @param icappId ICApp ID
     * @param path API endpoint path
     * @param data inbound data for the ICApp, is either a object or an array of objects.  For bulk operations you can
     *             send data as an array of objects.ICApp will be triggered for each object in the array.
     * @param rh callback rh see above
     */
    public void put(String icappId, String path, Json data, ResponseHandler rh) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(path, "path");
        Vld.checkEmpty(data, "data");
        Vld.checkNull(rh, "ResponseHandler");
        mustbeHttps();

        msghandler.setHandler(rh);
        msghandler.outbound(Message.UPDATE, new Message(API, _gwpath(path, icappId), data));
    }

    /**
     * Convenient method to invoke user defined PUT REST API
     *
     * @param icappId ICApp ID
     * @param path API endpoint path
     * @param data inbound data for the ICApp, is either a object or an array of objects.  For bulk operations you can
     *             send data as an array of objects.ICApp will be triggered for each object in the array.
     */
    public void put(String icappId, String path, Json data) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(path, "path");
        Vld.checkEmpty(data, "data");
        mustbeHttps();

        msghandler.setHandler(null);
        msghandler.outbound(Message.UPDATE, new Message(API, _gwpath(path, icappId), data));
    }

    /**
     * Convenient method to invoke user defined DELETE REST API
     *
     * @param icappId ICApp ID
     * @param path API endpoint path
     * @param data inbound data for the ICApp, is either a object or an array of objects.  For bulk operations you can
     *             send data as an array of objects.ICApp will be triggered for each object in the array.
     * @param rh callback rh see above
     */
    public void del(String icappId, String path, Json data, ResponseHandler rh) throws Exception {
        Vld.checkEmpty(icappId, "icappId");
        Vld.checkEmpty(path, "path");
        Vld.checkEmpty(data, "data");
        Vld.checkNull(rh, "ResponseHandler");
        mustbeHttps();

        msghandler.setHandler(rh);
        msghandler.outbound(Message.DELETE, new Message(API, _gwpath(path, icappId), data));
    }

    /**
     * Allow reading App/Device config (i.e. Shadow) attributes from the Server/Cloud platform
     * (i.e. reading config from app/device Shadow)
     * Request for multiple attributes can be included in a json object array.
     *
     *  {"n":"attribute-name"}
     *
     * Response is received via {@link ResponseHandler} ack() and would use the format
     *
     *  {"n":"attribute-name", "v":...}
     *
     *  if multiple attributes entries are requested, then the responses is returned as a
     *  json object array.
     *
     *  [{"n":"attribute-name", "v":...}, {"n":"attribute-name", "v":..., }, {...}]
     *
     * @param reqest JSON containing attributes names
     * @param rh callback rh instance of {@link ResponseHandler}
     * @throws Exception in case of exception
     */
    public void read(Json reqest, ResponseHandler rh) throws Exception{
        Vld.checkEmpty(reqest, "request");
        Vld.checkNull(rh, "ResponseHandler");
        mustbeHttps();
        msghandler.setHandler(rh);
        msghandler.outbound(Message.READ,  new Message(ATT, reqest));
    }

    /**
     * Allow updating App/Device config (i.e. Shadow) attributes on the Server/Cloud
     *
     *  {"n":"attribute-name", "v": attribute-value}
     *
     * Request for multiple attributes can be included in a json object array.
     *
     *  [{"n":"attribute-name", "v":...}, {"n":"attribute-name", "v":..., }, {...}]
     *
     * Response is received via {@link ResponseHandler} ack().
     *
     * @param reqest JSON containing attributes names/values
     * @throws Exception in case of exception
     */
    public void update(Json reqest) throws Exception{
        Vld.checkEmpty(reqest, "request");
        mustbeHttps();
        msghandler.setHandler(null);
        msghandler.outbound(Message.UPDATE,  new Message(ATT, reqest));
    }

    /**
     * Allow updating App/Device config (i.e. Shadow) attributes on the Server/Cloud platform
     *
     *  {"n":"attribute-name", "v": attribute-value}
     *
     * Request for multiple attributes can be included in a json object array.
     *
     *  [{"n":"attribute-name", "v":...}, {"n":"attribute-name", "v":..., }, {...}]
     *
     * Response is received via {@link ResponseHandler} ack().
     *
     * @param reqest JSON containing attributes names/values
     * @param rh callback rh instance of {@link ResponseHandler}
     * @throws Exception in case of exception
     */
    public void update(Json reqest, ResponseHandler rh) throws Exception{
        Vld.checkEmpty(reqest, "request");
        Vld.checkNull(rh, "ResponseHandler");
        mustbeHttps();
        msghandler.setHandler(rh);
        msghandler.outbound(Message.UPDATE,  new Message(ATT, reqest));
    }

    /**
     * For WSS/ MQTT transport's, client connect and start generating messages
     *
     * Here, response are ignored.
     * To receive responses you must use {@link Client#connect(String icappId, LifeCycleHandler lch)} instead of this method.
     *
     * @throws Exception    in case of exception
     */
    public void connect() throws Exception {
        if(this.transport == Transport.HTTPS) throw new IllegalStateException("Invalid transport!, must be WSS/MQTT");
        msghandler.connect(null);
    }

    /*
     * For WSS/ MQTT transport's, client connect and start processing messages when LifeCycleHandler is provided
     *
     * Clients who need responding to platform sent requests (via ICApps settings) e.g. for IoT devices or monitoring Apps,
     * need to connect using websocket/mqtt (WSS=1 and MQTT=2).
     *
     * Esp for MQTT, if you need to subscribe to server/cloud platform sent requests per ICApp basis,
     * you must provide an ICApp ID. This ICApp ID will be used for MQTT subscription. Otherwise client will subscribe to
     * all ICApp requests that this App/Device is used with.
     *
     * Responses are received via {@link LifeCycleHandler#ack(Json response)} or other server request via read/write/exec
     *
     * N.B. During the life span of this client with this {@link LifeCycleHandler}, you must provide same
     * {@link LifeCycleHandler} instance when calling any other method accepting {@link ResponseHandler}
     *
     * @param icappId   (optional) for websocket and is recommended for MQTT if subscribing to per ICApp basis (see above)
     * @param lch   callback handler instance of {@link LifeCycleHandler}
     * @throws Exception    in case of exception
     */
    public void connect(String icappId, LifeCycleHandler lch) throws Exception {
        if(this.transport == Transport.HTTPS)
            throw new IllegalStateException("Invalid transport!, explicit connect is required only for WSS/MQTT");
        Vld.checkNull(lch, "LifeCycleHandler");
        msghandler.setHandler(lch);
        msghandler.connect(icappId);
    }

    /**
     * Disconnect gracefully from the Server/Cloud platform
     *
     * @return true     if success
     * @throws Exception    in case of exception
     */
    public boolean disconnect() throws Exception{
        //if(this.transport == Transport.HTTPS) throw new IllegalStateException("Invalid transport!, must be WSS/MQTT");
        ResponseHandler rh = msghandler.getHandler();
        return rh != null? rh.shutdown() && msghandler.disconnect() : msghandler.disconnect();
    }

    /**
     * Allows to wait till interrupted. Clients can impl. {@link LifeCycleHandler#shutdown()}
     * to interrupt the behaviour.
     *
     * N.B. this method is used only if your app/device program needs a way to wait during the req/res loop
     *
     * @throws InterruptedException once interrupted
     */
    public void await() throws Exception {
        synchronized (this) {
            while (!disconnect()) {
                wait();
            }
        }
    }

    private void checkRH(ResponseHandler rh){
        if(rh == null) throw new IllegalArgumentException("ResponseHandler cannot be null!");
    }

    private void mustbeHttps(){
        if(this.transport != Transport.HTTPS) throw new IllegalStateException("Invalid transport!, must be HTTPS");
    }

    private String _gwpath(String path, String icappId){
        if(path == null || path.length() == 0 ) throw new IllegalArgumentException("path cannot be null/empty!");
        if(icappId == null || icappId.length() == 0 ) throw new IllegalArgumentException("ICApp ID cannot be null/empty!");

        return icappId+(path.startsWith("/")? path: "/"+path);
    }

    public boolean isReady() {
        return msghandler.getState() == State.READY;
    }
}
