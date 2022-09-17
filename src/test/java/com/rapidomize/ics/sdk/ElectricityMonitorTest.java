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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * kitchen: energy consumed by kitchen, e.g. having dishwasher, oven, microwave, refrigerator, and a light
 * laundry room: energy consumed by the laundry room, containing a washing-machine, a tumble-drier, and a light
 * heating: energy consumed  by an electric water-heater and an air-conditioner
 *
 */
public class ElectricityMonitorTest {


    static final String appId = "YOUR_APP_ID";
    static final String token = "YOUR_APP_API_KEY";
    static final String icappId = "ICAPP_ID";

    Timer timer = new Timer();

    @Before
    public void setup() throws Exception {
    }

    @Test
    public void onEvent() throws Exception {
        final Client client = new Client(appId, token, Transport.WSS);
        client.connect(null, new DeviceHandler(client));
        //following statement will block current thread and connect for events.
        client.await();
    }

    private class DeviceHandler implements LifeCycleHandler {
        Client client;

        public DeviceHandler(Client client) {
            this.client = client;
        }

        @Override
        public void connected() {
            //simulate entity's event thread
            System.out.println("connected");
            long recurrence = 10 * 1000;
            timer.scheduleAtFixedRate(new MonitorTask(client), new Date(), recurrence);
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
            System.out.println("Err:");
            e.printStackTrace();
        }
    }

    class MonitorTask extends TimerTask {
        Client client;
        Random rnd = new Random();
        public MonitorTask(Client client) {
            this.client = client;
        }

        double nextDouble(double origin, double bound) {
            double r = rnd.nextDouble();
            r = r * (bound - origin) + origin;
            if (r >= bound) // correct for rounding
                r = Math.nextDown(bound);
            return r;
        }

        @Override
        public void run() {
            System.out.println("run()");
            try {
                Json ev = new Json(true);
                ev.add(new Json("n", "electricity", "eq","kitchen", "v",nextDouble(14, 20)));
                ev.add(new Json("n", "electricity", "eq","laundry_room", "v",nextDouble(4, 9)));
                ev.add(new Json("n", "electricity", "eq","heating", "v",nextDouble(20, 30)));

//                client.trigger(icappId, ev);
                client.trigger_ev(icappId, ev);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}