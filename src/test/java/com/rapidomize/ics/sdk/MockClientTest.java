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
import com.rapidomize.ics.sdk.events.Message;
import com.rapidomize.ics.sdk.events.MessageHandler;
import com.rapidomize.ics.sdk.events.Handler;
import com.rapidomize.ics.sdk.transport.Transport;
import com.rapidomize.ics.sdk.common.Status;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.lang.reflect.Field;
import java.util.Random;



public class MockClientTest {
    static final String appId = "YOUR_APP_ID";
    static final String token = "YOUR_APP_API_KEY";
    static final String icappId = "ICAPP_ID";

    Json entity = new Json();
    //    String entity = "{\"n\":\"MyApp\", \"loc\":{\"lat\": 6.842425, \"lng\": 79.962595}, \"o\":[{\"n\":\"alarm\"}, {\"n\":\"temperature\"}, {\"n\":\"color\"}]}";
    String obs = "[{\"n\":\"loc\",\"val\":{\"lat\": 6.844535, \"lng\": 79.962595}},{\"n\":\"temperature\",\"val\":31.30}, {\"n\":\"color\",\"val\":\"red\"}]";

    @Before
    public void setup() throws Exception {
        entity.put("n", "MyApp");
        entity.put("loc", new Json("{\"lat\": 6.842425, \"lng\": 79.962595}"));
        entity.put("o", new Json("[{\"n\":\"alarm\"}, {\"n\":\"temperature\"}, {\"n\":\"color\"}]"));
        entity.toJson();
    }

    class MockAppHandler implements LifeCycleHandler {
        private final Logger logger = LoggerFactory.getLogger(MockAppHandler.class);
        Client client;
        Random rnd = new Random();
        boolean end=false;

        public MockAppHandler(Client client) {
            this.client = client;
        }

        @Override
        public void connected() {
            //simulate entity's event thread
            Thread entity = new Thread(() -> {
                boolean interrupted = false;
                while (!interrupted){

                    try {
                        //trigger some event
                        Json ev = new Json(true);
                        ev.add(new Json("{\"n\":\"loc\",\"loc\":{\"lat\": 6.844535, \"lng\": 79.962595}}"));
//                        ev.add(new Json("{\"n\":\"temperature\",\"val\":31.30}"));
//                        ev.add(new Json("{\"n\":\"color\",\"val\":\"red\"}"));

//                        client.trigger(ev);
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        interrupted = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            entity.start();
        }

        @Override
        public Json read(Json event) throws Exception {
            logger.info("read data: "+ event);
            return new Json("{\"n\":\"temperature\",\"val\":"+ rnd.nextFloat()+"}");
        }

        @Override
        public Status write(Json event) throws Exception {
            logger.info("write data: "+ event);
            return Status.SUCCESS;
        }

        @Override
        public Status exec(Json event) throws Exception {
            logger.info("exec data: "+ event);
            return Status.SUCCESS;
        }

        @Override
        public void ack(Json response) throws Exception {
            System.out.println("ack: "+ response.toJson());
        }

        @Override
        public boolean shutdown() {
            return false;
        }

        @Override
        public void onException(Exception e) {
            if(e.getMessage()!=null) logger.warn(e.getMessage());
            else logger.warn("",e);
        }
    }

    class MockTransport extends Transport {
        private final Logger logger = LoggerFactory.getLogger(MockTransport.class);
        boolean sse=false;

        public MockTransport() {
            super(new Handler() {
                @Override
                public void inbound(Message msg) throws Exception {
                }

                @Override
                public void outbound(byte code, Message msg) throws Exception {
                }
            });
        }

        public void setHandler(Handler mh){
            handler = mh;
            initSSE();
        }

        public void connect(){

        }

        @Override
        public void disconnect() throws Exception {

        }

        @Override
        public Object send(Message msg) throws Exception {
            logger.info("send json to: {}, msg: {}", msg.getUri(), msg.getPayload());

            Message recvmsg = new Message(Message.TYPE.SSM);
            recvmsg.setCode(Message.ACK);

            int contentLength=0;
            if(!msg.getUri().endsWith("/event") && !msg.getUri().endsWith("/ack")){
                contentLength = 1; //hack
                recvmsg.setPayload(new Json("id", "2485377957895fc3f5-6-7414e1bd5c59"));
            }else {
                sse=true;
                return true;
            }

            logger.info("received json with code: {}, msg: {}", recvmsg.getCode(), recvmsg.getPayload());

            recv(recvmsg);
            sse=true;
            return true;
        }

        @Override
        public void setAppId(String appId) {

        }

        @Override
        public void setToken(String tkn) {

        }

        void initSSE(){
            Message recvmsg = new Message(Message.TYPE.SSM);

            //simulate entity's event thread
            Thread entity = new Thread(() -> {
                boolean interrupted = false;
                while (!interrupted){

                    try {
                        if(!sse){
                            Thread.sleep(5000);
                            continue;
                        }

                        int contentLength=0;

                        //read
                        recvmsg.setCode(Message.READ);
                        recvmsg.setPayload(new Json("n", "temperature", "op", Message.READ, "mid", 1));

                        //write
//                        recvmsg.setCode(Message.WRITE);
//                        recvmsg.setPayload(String.format("{\"n\":\"color\", \"op\":%d, \"val\":\"blue\"}", Message.WRITE).getBytes());

                        //write
//                        recvmsg.setCode(Message.EXEC);
//                        recvmsg.setPayload(String.format("{\"n\":\"alarm\", \"op\":%d}", Message.EXEC).getBytes());
//                        recvmsg.setPayload(String.format("{\"n\":\"sms\", \"op\":%d, \"to\":\"123456\", \"msg\":\"Hi there\"}", Message.EXEC).getBytes());

                        logger.info("received json with code: {}, msg: {}", recvmsg.getCode(), recvmsg.getPayload());
                        recv(recvmsg);

                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        interrupted = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            entity.start();
        }
    }

    class TestMessageHandler extends MessageHandler{
        public TestMessageHandler(String token, String appId, int trans, Transport transport) {
            super(appId, token, trans, transport);
        }
    }

    @Test
    public void onEventTest() throws Exception {

        final Client client = new Client();
        Field handler = Client.class.getDeclaredField("handler");
        MockTransport motrans = new MockTransport();
        MessageHandler mh = new TestMessageHandler(token, appId, Client.DEFAULT_TRANSPORT, motrans);
        motrans.setHandler(mh);

        handler.set(client, mh);

        client.trigger(icappId, new Json("foo", "bar"));
        client.connect(icappId, new MockAppHandler(client));
        //following statement will block current thread and connect for events.
        client.await();
    }


}
