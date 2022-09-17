package com.rapidomize.ics.sdk.common;

/**
 */
public class Vld {
    public static void checkEmpty(String param, String name){
        if(param == null || param.isEmpty()) throw new IllegalArgumentException(name+" cannot be null/empty");
    }

    public static void checkEmpty(Json param, String name){
        if(param == null || param.isEmpty()) throw new IllegalArgumentException(name+" cannot be null/empty");
    }

    public static void checkNull(Object param, String name){
        if(param == null) throw new IllegalArgumentException(name+" cannot be null");
    }
}
