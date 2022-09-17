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
import org.junit.Test;

import java.util.Random;

public class APITest {

    static final String appId = "YOUR_APP_ID";
    static final String token = "YOUR_APP_API_KEY";
    static final String icappId = "ICAPP_ID";


    @Test
    public void apiTest() throws Exception {

        final Client client = new Client(appId, token);
        //No need to call client.connect();

        Random rnd = new Random();

        client.get(icappId, "/rows?rng=X1:Y1&st=1", new TestResponseHandler());
        client.post(icappId, "/rows", new Json("id", 123, "n", "asdsd", "val", rnd.nextInt(100)), new TestResponseHandler());

        client.await();
    }

    class TestResponseHandler implements ResponseHandler {

        @Override
        public void ack(Json response) throws Exception {
            System.out.println("response: " + response);
        }

        @Override
        public void onException(Exception e) {
            e.printStackTrace();
        }
    }
}
