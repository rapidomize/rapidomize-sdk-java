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

package com.rapidomize.ics.sdk.events;

import com.rapidomize.ics.sdk.common.Json;

import java.util.Arrays;


public class Message {

    /*
        Common request / response status code for different protocols -
        http, websocket, mqtt

        3-bit class and a 5-bit detail

        0 1 2 3 4 5 6 7
        +-+-+-+-+-+-+-+-+
        |major|  minor  |
        +-+-+-+-+-+-+-+-+

        e.g. response status
        1. 0x41 -> 010 00001  -> 2.01  => CREATED
        2. 0x9d -> 100 11101  -> 4.29  => TOO_MANY_REQUESTS
        3. 0xA5 -> 101 00101  -> 5.20  => UNKNOWN

    */
    /* operation (op-code) request code: */
    public static final byte NOP=0x00;
    public static final byte READ=0x01;                         /* 0.01 */
    public static final byte WRITE =(byte)0x02;                 /* 0.02 */
    public static final byte UPDATE =(byte)0x03;                /* 0.03 */
    public static final byte DELETE=(byte)0x04;                 /* 0.04 */
    public static final byte EXEC=(byte)0x05;                   /* 0.05 */
    public static final byte INFO=(byte)0x06;   				/* 0.06 request for metadata if any*/
    public static final byte SES=(byte)0x08;					/* 0.08 establish a session */

	/* Response status code */
    public static final byte CONTINUE=(byte)0x20;               /* 1.00 */
    public static final byte PROCESSING=(byte)0x22;             /* 1.02 async operation in progress */

    public static final byte ACK=(byte)0x40;                    /* 2.00 success */
    public static final byte CREATED=(byte)0x41; 				/* 2.01 */
    public static final byte ACCEPTED=(byte)0x42;               /* 2.02 request is accepted for processing */
    public static final byte CHANGED=(byte)0x44;				/* 2.04 delete/update operation success*/
    public static final byte NO_CONTENT=(byte)0x44;             /* 2.04 - same as above */

    public static final byte FOUND=(byte)0x62;				    /* 3.02 */
    public static final byte NOT_CHANGED=(byte)0x64;		    /* 3.04 update, delete operation failed and no changed is made */

    /** Client Error 4xx */
    public static final byte BAD_REQUEST=(byte)0x80;	        /* 4.00 */
    public static final byte UNAUTHORIZED=(byte)0x81;           /* 4.01 */
    public static final byte FORBIDDEN=(byte)0x83;		        /* 4.03 */
    public static final byte NOT_FOUND=(byte)0x84;		        /* 4.04 */
    public static final byte NOT_ACCEPTABLE=(byte)0x86;	        /* 4.06 */
    public static final byte REQUEST_TIMEOUT=(byte)0x88;        /* 4.08 */
    public static final byte CONFLICT=(byte)0x89;			    /* 4.09 */
    public static final byte GONE=(byte)0x8A;			        /* 4.10 */
    public static final byte INVALID_CONTENT_SIZE=(byte)0x8B;	/* 4.11 */
    public static final byte PRECONDITION_FAILED=(byte)0x8C;	/* 4.12 */
    public static final byte PAYLOAD_TOO_LARGE=(byte)0x8D;	    /* 4.13 */
    public static final byte UNSUPPORTED_MEDIA_TYPE=(byte)0x8F; /* 4.15 */
    public static final byte TOO_MANY_REQUESTS=(byte)0x9D;      /* 4.29 */

    /** Server Error 5xx */
    public static final byte INTERNAL_ERROR=(byte)0xA0;	        /* 5.00 */
    public static final byte BAD_GATEWAY=(byte)0xA2;		    /* 5.02 */
    public static final byte SERVICE_UNAVAILABLE=(byte)0xA3;	/* 5.03 */
    public static final byte GATEWAY_TIMEOUT=(byte)0xA4;		/* 5.04 */
    public static final byte INVALID_VERSION=(byte)0xA5;		/* 5.05 */
    public static final byte UNKNOWN=(byte)0xB4;		        /* 5.20 */

    public static final byte UNDEFINED=(byte)0xFF;

    byte ver;
    Integer mid; //message id

    String uri;
    String token; //FIXME: we must store the token in a char []
    byte code;
    String appId;

    //TODO: we must convert this to json as we deal with JSON only.
    Json payload;

    boolean duplicate;

    TYPE type;

    public enum TYPE{
        ICAPP,
        EVENT,
        BIN,
        ATT,
        API,
        SSE,
        ACK,
        SSM
    };

    public static final String MSG = "msg";
    public static final String URI = "uri";
    public static final String CODE = "op";
    public static final String MID = "mid";
    public static final String ERR = "err";
    public static final String MT_JSON = "application/json";
    public static final String MT_TXT = "text/plain";
    public static final String MT_CSV = "text/csv";
    public static final String MT_BIN = "application/octet-stream";

    public static String EV="/event";
    public static String BIN="/upload";

    public Message() {
    }

    public Message(TYPE type) {
        this.type = type;
    }

    public Message(TYPE type, String uri, Json data) {
        if(type == TYPE.EVENT) {
            uri += EV;
            type = TYPE.ICAPP;
        }

        if(type == TYPE.BIN){
            uri += BIN;
            type = TYPE.ICAPP;
        }

        this.type = type;
        this.uri = uri;
        this.payload = data;
    }

    public Message(TYPE type, Json data) {
        this(type, null, data);
    }

    public Message(String uri , Json data) {
        this(TYPE.ICAPP, uri, data);
    }


    public Integer getMid() {
        return mid;
    }

    public void setMid(Integer mid) {
        if(this.mid==null)
            this.mid = mid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public byte getCode() {
        return code;
    }

    public void setCode(byte code) {
        str(code);//validate
        this.code = code;
    }

    public Json getPayload() {
        return payload;
    }

    public void setPayload(Json payload) {
        this.payload = payload;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public TYPE getType() {
        return type;
    }

    public boolean isOps(){
        return code >= READ && code <= INFO;
    }

    public boolean isError(){
        return (code >> 5  & 0x5) >= 4 ;
    }

    public boolean isSuccess(){
        return (code >> 5  & 0x5) < 4 ;
    }

    public boolean  isClientError(){
        return (code >> 5  & 0x5) == 4 ;
    }

    public boolean  isServerError(){
        return (code >> 5  & 0x5) > 4 ;
    }

    @Override
    public String toString() {
        return '{' +
                "ver:" + ver + ',' +
                ", mid:" + mid + ',' +
                ", uri:" + uri + ',' +
                ", token:" + token + ',' +
                ", code:" + code + ',' +
                ", appId:" + appId + ',' +
                ", payload:" + payload + ',' +
                ", duplicate:" + duplicate + ',' +
                ", type:" + type +
                '}';
    }

    public static String str(byte code){
        switch (code){
            case NOP: return "NOP";
            case READ: return "READ";
            case WRITE : return "WRITE ";
            case UPDATE : return "UPDATE ";
            case DELETE: return "DELETE";
            case EXEC: return "EXEC";
            case INFO: return "INFO";
            case SES: return "SES";

            /* Response status code */
            case CONTINUE: return "CONTINUE";
            case PROCESSING: return "PROCESSING";

            case ACK: return "ACK";
            case CREATED: return "CREATED";
            case ACCEPTED: return "ACCEPTED";
            case CHANGED: return "CHANGED";
//            case NO_CONTENT: return "case NO_CONTENT";

            case FOUND: return "FOUND";
            case NOT_CHANGED: return "NOT_CHANGED";

            /** Client Error 4xx */
            case BAD_REQUEST: return "BAD_REQUEST";
            case UNAUTHORIZED: return "UNAUTHORIZED";
            case FORBIDDEN: return "FORBIDDEN";
            case NOT_FOUND: return "NOT_FOUND";
            case NOT_ACCEPTABLE: return "NOT_ACCEPTABLE";
            case REQUEST_TIMEOUT: return "REQUEST_TIMEOUT";
            case CONFLICT: return "CONFLICT";
            case GONE: return "GONE";
            case INVALID_CONTENT_SIZE: return "INVALID_CONTENT_SIZE";
            case PRECONDITION_FAILED: return "PRECONDITION_FAILED";
            case PAYLOAD_TOO_LARGE: return "PAYLOAD_TOO_LARGE";
            case UNSUPPORTED_MEDIA_TYPE: return "UNSUPPORTED_MEDIA_TYPE";
            case TOO_MANY_REQUESTS: return "TOO_MANY_REQUESTS";

            /** Server Error 5xx */
            case INTERNAL_ERROR: return "INTERNAL_ERROR";
            case BAD_GATEWAY: return "BAD_GATEWAY";
            case SERVICE_UNAVAILABLE: return "SERVICE_UNAVAILABLE";
            case GATEWAY_TIMEOUT: return "GATEWAY_TIMEOUT";
            case INVALID_VERSION: return "INVALID_VERSION";
            case UNKNOWN: return "UNKNOWN";

            case UNDEFINED: return "UNDEFINED";

            default: throw new IllegalArgumentException("invalid code");
        }
    }
}
