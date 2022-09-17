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
import com.rapidomize.ics.sdk.transport.Transport;

import org.junit.Test;

import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class IngestionTest {
    static final String appId = "YOUR_APP_ID";
    static final String token = "YOUR_APP_API_KEY";
    static final String icappId = "ICAPP_ID";

    @Test
    public void ingest() throws Exception {

        final Client client = new Client(appId, token);
//        client.connect();
        Random rnd = new Random();
        int cnt=0;

        while (cnt < 100){
            try {
                System.out.println("id: "+cnt);;
                client.trigger(icappId, new Json("id", cnt++,"test", rnd.nextInt(100)));

                //no delay
//                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void ingestArrayToICApps() throws Exception {


        final Client client = new Client(appId, token);
//        client.connect();

        Random rnd = new Random();
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask () {
            @Override
            public void run() {
                for(int i=0;i<5;i++) {
                    try {
                        client.trigger(icappId, new Json()
                                .add(new Json("test", rnd.nextDouble()))
                                .add(new Json("foo", rnd.nextInt())));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Date(), 10 * 1000);

        //wait till interrupted
        System.in.read();
    }


    @Test
    public void issueReporting() throws Exception {


        final Client client = new Client(appId, token);
//        client.connect();

        String [] st ={"SUCCESS", "FAILED", "UNKNOWN"};
        Random rnd = new Random();
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask () {
            int i=0;
            @Override
            public void run() {

                //for (int i = 0; i < 5; i++) {
                    try {
                        client.trigger(icappId, new Json("time", i + 15, "status", st[rnd.nextInt(2)]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    i++;
                //}
            }
        }, new Date(), 10 * 1000);

        //wait till interrupted
        System.in.read();
    }


    @Test
    public void ingestICAppMqtt() throws Exception {

        final Client client = new Client(appId, token, Transport.MQTT);
        client.connect();
        Random rnd = new Random();
        int cnt=0;

        while (cnt < 2){
            try {
                client.trigger(icappId, new Json("id", cnt++,"test", rnd.nextInt(100)));
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*new Timer().scheduleAtFixedRate(new TimerTask () {
            int cnt=0;
            @Override
            public void run() {
                try {
                    client.trigger(icappId, new Json("id", cnt++,"test", rnd.nextInt(100)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Date(), 10);*/

        //wait till interrupted
        //System.in.read();
    }


}
