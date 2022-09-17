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

package com.rapidomize.ics.sdk.transport;

import com.rapidomize.ics.sdk.common.Json;
import com.rapidomize.ics.sdk.events.Handler;
import com.rapidomize.ics.sdk.events.Message;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.rapidomize.ics.sdk.events.Message.*;

final class Mqtt extends Transport implements MqttCallbackExtended {

    private  final Logger logger = LoggerFactory.getLogger(Mqtt.class);

    MqttConnectOptions opt = new MqttConnectOptions();
    MqttClient subscriber = null;
    MqttClient publisher;

    String subTopic;
    String epUri;
    boolean reconnect = false;

    Mqtt(String appId, String token, Handler handler) {
        super(handler);
        this.token = token;
        this.appId = appId;
    }

    public void connect(){
        logger.debug("Attempting to connect ...");
        try {
            final String ephost = CONF.getProperty("ep.host");
            if(ephost == null || ephost.isEmpty())
                throw new IllegalArgumentException("invalid uri, cannot be null/empty");
            epUri = "ssl://"+ephost+":8883";

            //epUri = CONF.getProperty("mqtt.endpoint.uri");
            logger.debug("server: {}", epUri);
            opt.setSocketFactory(getSocketFactory());
            opt.setCleanSession(true);
            opt.setAutomaticReconnect(true);
            opt.setUserName(appId);
            opt.setPassword(token.toCharArray());
            opt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

            //FIXME: can we use same clientId for both pub/sub as per [MQTT-3.1.4-2]
            //exception is that we allow this in our impl. of MQTT 3.1.1
            subscriber = new MqttClient(epUri, appId);
            _connect(subscriber, opt);
            subscriber.setCallback(this);

            publisher = new MqttClient(epUri, appId);
            publisher.setCallback(this);
            _connect(publisher, opt);

            reconnect = false;
        } catch (Exception e) {
            logger.error("unable to establish MQTT pipeline", e);
            throw new IllegalStateException(e);
        }
        logger.debug("successfully connected!");
    }

    void _connect(MqttClient client, MqttConnectOptions opt){
        boolean connected=false;
        while(!connected) {
            try {
                client.connect(opt);
                connected = client.isConnected();
                ebo.reset();
            }catch (MqttSecurityException e){
                logger.error("Cannot connect: {}", e.getMessage());
                throw new IllegalStateException(e);
            } catch (MqttException e) {
                logger.warn("unable to establish MQTT pipeline with server {}, ... retrying - cause: {}", epUri,
                        e.getCause() == null?e.getMessage():e.getCause().getMessage());
                if(ebo.shouldRetry()) continue;
                break;
            }
        }
    }

    @Override
    public void connectComplete(boolean b, String s) {
        Message recvmsg = new Message(Message.TYPE.SSM);
        try {
            if(connect) {
                subTopic = icappId != null? Handler.ICAPP_EP_PATH + icappId : Handler.EP_PATH + appId;
                subscriber.subscribe(subTopic + "/#");
            }

            if(!reconnect) {
                recvmsg.setCode(Message.ACK);
                recv(recvmsg);
            }
            reconnect = false;
        } catch (Exception e) {
            logger.error("subscribing: ", e);
            recvmsg.setCode(Message.UNKNOWN);
            try {
                recv(recvmsg);
            } catch (Exception e1) {
                logger.error("Unexpected: ", e1);
            }
        }
    }

    @Override
    public void connectionLost(Throwable t) {
        //logger.debug("lost connection to MQTT broker", t);
        try {
            if(!subscriber.isConnected()) {
                //logger.debug("reconnecting");
                reconnect = true;
                subscriber.reconnect();
            }
        } catch (MqttException e) {
            logger.error("lost connection to MQTT broker {}", e.getMessage());
            throw new IllegalStateException(e);
        }

        try {
            if(!publisher.isConnected()) {
                //logger.debug("reconnecting");
                reconnect = true;
                publisher.reconnect();
            }
        } catch (MqttException e) {
            logger.error("lost connection to MQTT broker {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMsg) throws Exception {
        logger.debug("Topic: "+topic+" Msg: "+mqttMsg);

        try {

            Message recvmsg = new Message(Message.TYPE.SSM);

            String [] parts = topic.split("/");
            if(parts.length >= 5)
                recvmsg.setAppId(parts[4]);
            recvmsg.setMid(mqttMsg.getId());
            recvmsg.setDuplicate(mqttMsg.isDuplicate());
            recvmsg.setUri(topic);

            final byte [] pl = mqttMsg.getPayload();
            int contentLength= pl.length;
            if(contentLength > 0) {
                Json payload = Json.fromJson(pl);

                final Long op = payload.getLong(CODE);
                final String err = payload.getString(ERR);
                if (op != null) {
                    recvmsg.setCode((byte) (long) op);
                }else if(payload.size() == 0) {
                    recvmsg.setCode(Message.ACK);
                }else if (err != null)
                    recvmsg.setCode(Message.UNKNOWN);

                //with mqtt we only use json as media type
                //recvmsg.setUri(payload.getString(URI));
                recvmsg.setPayload((err==null)?payload.getJson(MSG) : new Json("err", err));
            }
            //TODO: need to make sure if the data is of a given content type. assume json
            //nak("Invalid response media type");
            byte code = recvmsg.getCode();
            logger.debug("received msg code: {}, msg: {} ",
                    Message.str(code),
                    (contentLength > 0? recvmsg.toString():""));

            recv(recvmsg);
        }catch (IllegalArgumentException e){
            logger.info("", e);
        }catch (Throwable t){
            logger.error("unexpected", t);
        }
    }

    @Override
    public Object send(Message msg) throws Exception {
        if(msg.getPayload() == null) throw new IllegalArgumentException("empty payload");
        //if(msg.getCode() < 0) throw new IllegalArgumentException("invalid message code: "+ msg.getCode());

        if(!publisher.isConnected()){
            connect();
        }

        //TODO: improve this.
       /* final String uri = msg.getUri();
        //String [] urif = msg.getUri().split("/");
        int loc = uri.lastIndexOf('/');

        String topic = uri.endsWith(Message.EV)? uri.substring(uri.lastIndexOf('/', loc))
                            : uri.substring(loc);*/

        final String topic =  msg.getUri();
        logger.debug("publish json to: {}, msg: {}", topic, msg.getPayload());
        MqttMessage mqttMessage = new MqttMessage(msg.getPayload().toString().getBytes());
        mqttMessage.setId(msg.getMid());
        mqttMessage.setQos(0);
        mqttMessage.setRetained(false);

        publisher.publish(topic, mqttMessage);
        return true;
    }

    @Override
    public void setAppId(String appId) {
        //NoP
    }

    @Override
    public void setToken(String tkn) {
        //NoP
    }

    void nak(String [] parts, int mId, String smsg) throws Exception {
        final byte NAK=0x07;

        Message msg = new Message();
        msg.setMid(mId);
        msg.setPayload(new Json("err", smsg));
        msg.setCode(NAK);
        msg.setToken(parts[2]);
        if(parts.length > 4) msg.setAppId(parts[4]);
        send(msg);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        logger.debug("delivery complete for topic & sp the token {}", iMqttDeliveryToken);
        /*try {
            listener.onMessage(null);
        } catch (Exception e) {
            logger.error("on reporting delivery complete event");
        }*/
    }

    public void disconnect() throws Exception{
        if(publisher.isConnected())
            publisher.close();
        if(subscriber.isConnected())
            subscriber.close();
    }
}
