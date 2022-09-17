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
import org.junit.Before;
import org.junit.Test;


public class MqttAgentTest {
    static final String appId = "YOUR_APP_ID";
    static final String token = "YOUR_APP_API_KEY";
    static final String icappId = "ICAPP_ID";

    Client client;

    @Before
    public void setup() throws Exception {
        client = new Client(appId, token, Transport.MQTT);
    }

    @Test
    public void testMQTTAgent() throws Exception {


        client.connect(icappId, new LifeCycleHandler() {

            int req=0;
            @Override
            public void connected() {
                //NoP since app is already connected.
                System.out.println("ready");
            }

            @Override
            public Json read(Json event) throws Exception {
                req++;
                System.out.println(req + " read data: "+ event);
                return null;
            }

            @Override
            public Status write(Json event) throws Exception {
                req++;
                System.out.println(req + " write data: "+ event);
                return Status.SUCCESS;
            }

            @Override
            public Status exec(Json event) throws Exception {
                req++;
                System.out.println(req + " exec data: "+ event);
                return Status.SUCCESS;
            }

            @Override
            public void ack(Json response) throws Exception {
                req++;
                System.out.println(req + " ack: "+ response);
            }

            @Override
            public void onException(Exception e) {
                e.printStackTrace();
            }
        });

        client.await();
    }
}
