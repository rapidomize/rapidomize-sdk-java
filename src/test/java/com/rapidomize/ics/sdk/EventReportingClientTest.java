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



public class EventReportingClientTest {
    private final Logger logger = LoggerFactory.getLogger(EventReportingClientTest.class);

    static final String appId = "YOUR_APP_ID";
    static final String token = "YOUR_APP_API_KEY";
    static final String icappId = "ICAPP_ID";

    String obs = "[{\"n\":\"/cur\",\"v\":35.32},{\"n\":\"/cur\",\"v\":34.50, \"t\":-10}, {\"n\":\"/max\",\"v\":38.11}]";

    @Test
    public void onEvent() throws Exception {

        final Client client = new Client(appId, token);

        //simulate event reporting thread
        Thread entity = new Thread(() -> {
            boolean interrupted = false;
            while (!interrupted){

                try {
                    //trigger some event
                    client.trigger_ev(icappId, obs);
                    Thread.sleep(5000);
                } catch (Exception e) {
                    logger.error("creating event: ", e);
                }
            }
        });

        entity.start();

        //following statement will block current thread and connect for events.
        client.await();
    }
}