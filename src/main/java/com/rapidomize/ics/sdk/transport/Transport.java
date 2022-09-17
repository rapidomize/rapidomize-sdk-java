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

import com.rapidomize.ics.sdk.common.Conf;
import com.rapidomize.ics.sdk.common.ExpBackOff;
import com.rapidomize.ics.sdk.events.Handler;
import com.rapidomize.ics.sdk.events.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public abstract class Transport {
    private  final Logger logger = LoggerFactory.getLogger(Transport.class);
    static final Conf CONF = Conf.getInstance();
    private static TrustManagerFactory tmf = null;

    protected String appId;
    protected String token; //FIXME: this has security issue due to string form storage.
    protected String icappId; //connectivity may be for a single ICApp


    protected Handler handler; //i.e. MessageHandler

    protected boolean connect=false;

    ExpBackOff ebo = new ExpBackOff();

    public static final int HTTPS = 0;
    public static final int WSS = 1; //WS uses param based auth
    public static final int MQTT = 2;

    public static final int AUTH_BASIC = 0;
    public static final int AUTH_TOKEN = 1;

    int type;

    protected Transport(){
    }

    protected Transport(Handler handler) {
        if(handler == null) throw new IllegalArgumentException("msghandler cannot be null");
        this.handler = handler;
    }

    public static Transport getInstance(int trans, String appId, String token, Handler handler) {
        //to start with registry
        switch (trans) {
            case HTTPS:
            case WSS: return new Http(trans, appId, token, handler);
            case MQTT: return new Mqtt(appId, token, handler);
            default:
                throw new IllegalStateException("unknown transport type - 'transport.type' config property must be set!");
        }
    }

    //Only expected to call by the Handler
    public void connect(String icappId, boolean hasRH) throws Exception{
        this.icappId = icappId;
        this.connect = hasRH;
        connect();
    }

    protected abstract void connect() throws Exception;
    public abstract void disconnect() throws Exception;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        disconnect();
    }

    public abstract Object send(Message msg) throws Exception;

    protected void recv(Message msg) throws Exception {
        handler.inbound(msg);
    }

    final TrustManagerFactory getTrustManagerFactory() throws Exception {
        if(tmf!=null) return tmf;

        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        String cafile = CONF.getProperty("cafile");
        if(cafile==null) cafile = "ca.pem";
        try(InputStream caInput = getClass().getClassLoader().getResourceAsStream(cafile)) {
            ks.load(null);
            Certificate certificate = certFactory.generateCertificate(caInput);
            ks.setCertificateEntry("ca", certificate);
        }
        tmf.init(ks);

        return tmf;
    }

    //TLS 1.0 & 1.1 support will be removed
    SSLSocketFactory getSocketFactory() throws Exception {
        SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
        sslCtx.init(null, getTrustManagerFactory().getTrustManagers(), null);
        return sslCtx.getSocketFactory();
    }

    public abstract void setAppId(String appId);
    public abstract void setToken(String tkn) throws Exception;
}
