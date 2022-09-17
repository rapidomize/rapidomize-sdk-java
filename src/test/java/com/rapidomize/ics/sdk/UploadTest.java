package com.rapidomize.ics.sdk;

import com.rapidomize.ics.sdk.common.Json;
import com.rapidomize.ics.sdk.common.MediaType;
import com.rapidomize.ics.sdk.common.Status;
import com.rapidomize.ics.sdk.transport.Transport;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;

/**
 */
public class UploadTest {
    static final String appId = "YOUR_APP_ID";
    static final String token = "YOUR_APP_API_KEY";
    static final String icappId = "ICAPP_ID";

    Client client;

    @Before
    public void setup() throws Exception {
        client = new Client(appId, token);
    }

    @Test
    public void uploadFile() throws Exception {

        try (
            InputStream file = getClass().getResourceAsStream("/image.jpg");

        ) {

            //e.g. reporting an incident with a image and comments for the event
            // 'file' attribute is mandatory and it represents the file name
            Json fileEv = new Json("file", "car.jpg", "comment", "A tree fell on a car in Northwest City " +
                                    "during a severe storm yesterday afternoon", "type", MediaType.JPG);

            //upload file with the event data.
            client.upload(icappId, file, fileEv, new ResponseHandler() {

                @Override
                public void ack(Json response) throws Exception {
                    System.out.println("file sending: "+response);
                }

                @Override
                public void onException(Exception e) {
                    e.printStackTrace();
                }
            });

            //wait till response from the server returns
            System.in.read();
        }


    }
}
