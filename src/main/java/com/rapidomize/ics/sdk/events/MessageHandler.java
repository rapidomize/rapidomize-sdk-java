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

package com.rapidomize.ics.sdk.events;

import com.rapidomize.ics.sdk.ResponseHandler;
import com.rapidomize.ics.sdk.common.Conf;
import com.rapidomize.ics.sdk.common.Json;
import com.rapidomize.ics.sdk.common.State;
import com.rapidomize.ics.sdk.transport.Transport;
import com.rapidomize.ics.sdk.LifeCycleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static com.rapidomize.ics.sdk.transport.Transport.HTTPS;
import static com.rapidomize.ics.sdk.transport.Transport.MQTT;
import static com.rapidomize.ics.sdk.transport.Transport.WSS;


public class MessageHandler implements Handler {
    private final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    //public static final String BASE_EP_PATH = "/api/v1/mo/";//CONF.getProperty("ep.path");
    //public static final String AGW_PATH= "/api/v1/agw/";//CONF.getProperty("ep.gw.path");

    protected Transport transport;

    //TODO: having all messages in a queue will block concurrent behaviour that may cause important messages to be processed with delay.
    AtomicInteger seq = new AtomicInteger();

    //app id to connect on the server responses.
    String appId;
    volatile State state = State.REGISTER_ACK;

    //FIXME: we must store the token in a char []
    String token;
    Json cnf;
    protected ResponseHandler handler;

    protected MessageHandler() {

    }

    protected MessageHandler(String appId, String token, int trans, Transport transport) {
        this();
        this.token = token;
        if(appId != null) {
            this.appId = appId;
        }

        try {
            //String uriProp;
            switch (trans){
                case HTTPS: state = State.READY; break;
                case WSS:
                case MQTT: break;
                default:
                    throw new IllegalArgumentException("invalid transport type, it must be either Transport.HTTPS (0), Transport.WSS (1) or Transport.MQTT (2)");
            }
            //EP_PATH = new URI(CONF.getProperty(uriProp)).getPath();

            this.transport = transport == null? Transport.getInstance(trans, this.appId, this.token, this): transport;

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public MessageHandler(String appId, String token, int trans){
        this(appId, token, trans, null);
    }

    public void connect(String icappId) throws Exception {
        this.transport.connect(icappId, this.handler != null);
    }

    public boolean disconnect() throws Exception {
        this.transport.disconnect();
        return true;
    }

    public void setHandler(ResponseHandler handler) {
        this.handler = handler;
    }

    public ResponseHandler getHandler() {
        return handler;
    }

    public String toUri(Message msg){
        String muri = msg.getUri();
        switch (msg.type){
            case API: return AGW_PATH+muri;
            case ICAPP:
            case EVENT:
            case BIN: return BASE_EP_PATH + msg.type.name().toLowerCase()+(muri != null?("/"+muri):"");
            default: return EP_PATH + appId+"/"+msg.type.name().toLowerCase()+(muri != null?("/"+muri):"");
        }
        /*return (msg.type == Message.TYPE.API)? AGW_PATH+muri
                    : EP_PATH + appId+"/"+msg.type.name().toLowerCase()+(muri != null?("/"+muri):"");*/
    }

    /*
        notify activity
        response to cmd by - ack, nak, with/without payload.
     */
    public void outbound(byte code, Message msg) throws Exception {
        msg.setMid(seq.getAndIncrement());
        msg.setCode(code);
        msg.setToken(token);
        msg.setAppId(appId);
        msg.setUri(toUri(msg));

        transport.send(msg);
    }

    public void inbound(Message msg) throws Exception{
        if(msg == null) throw new IllegalArgumentException("msg cannot be null");
        byte code = msg.getCode();
        logger.debug("code: {}", code);

        try {

            if(handler == null && msg.isOps()) {
                noLCH(msg);
                return;
            }

            Json pl= msg.getPayload();

            Json res;
            switch (code) {
                case Message.READ:
                    if(handler instanceof LifeCycleHandler) {
                        res = ((LifeCycleHandler) handler).read(pl);
                        outbound(res, msg);
                    }else noLCH(msg);
                    break;
                case Message.WRITE:
                case Message.UPDATE:
                case Message.DELETE:
                    if(handler instanceof LifeCycleHandler) {
                        res = new Json();
                        res.put("status", ((LifeCycleHandler) handler).write(pl).name());
                        outbound(res, msg);
                    }else noLCH(msg);
                    break;
                case Message.EXEC:
                    if(handler instanceof LifeCycleHandler) {
                        res = new Json();
                        res.put("status", ((LifeCycleHandler) handler).exec(pl).name());
                        outbound(res, msg);
                    }else noLCH(msg);
                    break;
                case Message.ACK:
                case Message.CREATED:
                case Message.ACCEPTED:
                case Message.NO_CONTENT:
                    if (state == State.REGISTER_ACK) {
                        state = State.READY;
                        logger.debug("{}", state.name());
                        //start a thread to avoid implementer blocking the internal loop.
                        if(handler instanceof LifeCycleHandler) {
                            new Thread(() -> {
                                try {
                                    ((LifeCycleHandler) handler).connected();
                                }catch (Exception e){
                                    logger.warn("Failed during connected callback",e);
                                }
                            }).start();
                        }
                    }else
                        if(handler != null)
                            handler.ack(pl);
                        else if(pl!=null)
                            logger.debug("received: {}", pl.toJson());
                    break;
                default:
                    if (state == State.REGISTER_ACK) {
                        throw new IllegalStateException("establishing server connection failed! ... ");
                    }
                    //TODO: what to do otherwise? for now just log error
                    if(handler != null)
                        handler.ack(pl);
                    else if(pl!=null)
                        logger.warn("{}", pl);
            }
        }catch (Exception e){
            logger.warn("inbound msg failed: {}", e.getMessage());
            if(handler != null)
                handler.onException(e);
            //in case this is a server/cloud platform request send a NAK
            if(code <= Message.EXEC && code >= Message.READ) {
                Message rmsg = new Message(Message.TYPE.ACK, new Json("msg", e.getMessage()));
                outbound(Message.INTERNAL_ERROR, rmsg);
            }
        }
    }

    void outbound(Json res, Message msg) throws Exception {
        res.put("mid", msg.getMid());
        final String uri = msg.getUri();
        if(uri!=null && !uri.isEmpty() && uri.startsWith("/api/v1/icapp/")){
            String up = uri.substring("/api/v1/icapp/".length());
            if(up.indexOf("/svc/") > 0 && (up = up.replace("svc","ack")).length() > 0 ){
                Message out = new Message(Message.TYPE.ICAPP, up, res);
                out.setMid(msg.getMid());
                outbound(Message.ACK, out);
                return;
            }
        }
        outbound(Message.ACK, new Message(Message.TYPE.ACK, res));
    }

    private void noLCH(Message msg) throws Exception {
        outbound(Message.FORBIDDEN, new Message(Message.TYPE.ACK, new Json("err", "No LifeCycleHandler Registered for " + msg.getAppId())));
        logger.error("No LifeCycleHandler Registered for {} to handle payloads!", msg.getAppId());
    }

    public State getState() {
        return state;
    }
}
