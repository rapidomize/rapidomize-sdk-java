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
import com.rapidomize.ics.sdk.transport.Transport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;


public class DeviceClientTest {

    private final Logger logger = LoggerFactory.getLogger(DeviceClientTest.class);

    static final String appId = "YOUR_APP_ID";
    static final String token = "YOUR_APP_API_KEY";
    static final String icappId = "ICAPP_ID";


    @Test
    public void testTrigger() throws Exception {

        final Client client = new Client(appId, token);
        //No need to call client.connect(); unless WS/MQTT.
        Random rnd = new Random();
        int cnt=0;

        //Trigger a ICApp
        client.trigger(icappId, "{\"a\":\"b\"}");

        //Trigger a ICApp having a response.
        client.trigger(icappId, new Json("id", cnt++, "n", "asdsd", "val", rnd.nextInt(100)), new ResponseHandler() {
            boolean shutdown=false;

            @Override
            public void ack(Json response) throws Exception {
                System.out.println("response: "+response);
                shutdown = true;
            }

            @Override
            public boolean shutdown() {return shutdown;}

            @Override
            public void onException(Exception e) {
                e.printStackTrace();
            }
        });

        //wait till response from the server returns
        client.await();
    }

    @Test
    public void testDeviceAttributes() throws Exception {

        final Client client = new Client(appId, token);
        //No need to call client.connect(); unless WS/MQTT.
        Random rnd = new Random();
        int cnt=0;

        //read app/device config (i.e. Shadow) attributes
        client.read(new Json("n", "fcmId"), new ResponseHandler() {

            boolean shutdown=false;
            @Override
            public void ack(Json response) throws Exception {
                System.out.println("response: "+response);

                //update app/device config (i.e. Shadow) attributes
                client.update(new Json("n", "fcmId", "v", "xyz123"));

            }

            @Override
            public boolean shutdown() {return shutdown;}

            @Override
            public void onException(Exception e) {
                e.printStackTrace();
            }
        });


        //wait till response from the server returns
        client.await();
    }

    @Test
    public void testEventTrigger() throws Exception {


        final Client client = new Client(appId, token, Transport.WSS);
        client.connect(null, new DeviceActionHandler(client));

        //following statement will block current thread and connect for events.
        client.await();
    }



    private class DeviceActionHandler implements LifeCycleHandler {
        Client client;

        public DeviceActionHandler(Client client) {
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
                        ev.add(new Json("n","loc","loc", new Json("lat", 6.844535, "lng", 79.962595)));
                        ev.add(new Json("n","temperature","v",31.30));
                        ev.add(new Json("n","color","vs","red"));
                        ev.add(new Json("n","humidity","v",65.32));

                        logger.debug("sending {}", ev);

                        client.trigger_ev(icappId, ev);

                        Thread.sleep(5000);
                    } catch (Exception e) {
                        logger.error("creating event: ", e);
                    }
                }
            });

            entity.start();
        }

        @Override
        public Json read(Json event) throws Exception {
            System.out.println("read data: "+ event);
            return null;
        }

        @Override
        public Status write(Json event) throws Exception {
            System.out.println("write data: "+ event);
            return Status.SUCCESS;
        }

        @Override
        public Status exec(Json event) throws Exception {
            System.out.println("exec data: "+ event);
            return Status.SUCCESS;
        }

        @Override
        public void ack(Json response) throws Exception {
            System.out.println("ack: "+ response);
        }

        @Override
        public boolean shutdown() {
            return false;
        }

        @Override
        public void onException(Exception e) {
            e.printStackTrace();
        }
    }
}