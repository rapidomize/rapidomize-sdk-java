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

package com.rapidomize.ics.sdk.common;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

public class Json extends LinkedHashMap<String, Object> implements Serializable{

    List<Object> array = null;

    //    static final DateFormat df = new ISO8601DateFormat();
    /*
         mapper.configOverride(Date.class)
            .setFormat(JsonFormat.Value.forPattern("yyyy.dd.MM"));
     */

    /**
     * Streaming JSON parsing for efficiency. of cause it is not flexible but this is the best for this engine.
     */
    static final JsonFactory factory;
    static {
        factory = new JsonFactory();
        factory.setCodec(new ObjectMapper());
    }

    public Json() {
    }

    public Json(boolean isArray) {
        if(!isArray) throw new IllegalArgumentException("Cannot defined an array isArray=false");
        array = new ArrayList<>();
    }

    public Json(List<Object> array) {
        if(array == null) throw new IllegalArgumentException("array cannot be null");
        this.array = array;

    }

 	public Json(Json js) {
        if(js == null) throw new IllegalArgumentException("json cannot be null");
        if(js.isArray()) array = js.getArray();
        else putAll(js);
    }

    public Json(String json) throws IOException {
        Json js = fromJson(json);
        if(js.isArray()) array = js.getArray();
        else putAll(js);
    }

    //construct a json array from string data
    public Json(boolean isArray, String data, String delimiter) throws IOException {
        this(isArray);
    }

    public Json(InputStream jsons) throws IOException {
        Json js = fromJson(jsons);
        if(js.isArray()) array = js.getArray();
        else putAll(js);
    }

    //allow creating Json with name/value pairs
    public Json(String[] keys, Object[] values){
        if (keys == null || values== null) throw new IllegalArgumentException("Arguments cannot be null");
        if(keys.length != values.length) throw new IllegalArgumentException("argument must be key/value pairs, so must have equal number of keys/values");

        for(int i=0; i < keys.length; i++){
            put(keys[i], values[i]);
        }
    }

    //allow creating Json with key/value pairs. every other element in the argument are keys or values. all keys must be strings
    //e.g. new Json("a", 1, "b", 2) -> {"a":1, "b":2}.
    public Json(Object ... kv){
        if (kv== null) throw new IllegalArgumentException("kv cannot be null");
        if(kv.length % 2 != 0) throw new IllegalArgumentException("argument must be key/value pairs");
        for(int i=0; i < kv.length; i++){
            if(!(kv[i] instanceof String)) throw new IllegalArgumentException("keys must be strings");
            put((String)kv[i++], kv[i]);
        }
    }


    public Json(Map<String, String> mp) {
        putAll(mp);
    }

    public boolean isArray(){
        return array != null;
    }

    public String getString(String key){
        return (String) get(key);
    }

    public Integer getInt(String key){
        return (Integer) get(key);
    }

    public Number getNumber(String key){
        return (Number) get(key);
    }

    public Long getAsLong(String key){
        Object v = get(key);
        Long ret = null;
        if(v != null) {
            if (v instanceof Long) {
                ret = (Long) v;
            } else if (v instanceof Integer) {
                ret = ((Integer) v).longValue();
            } else if (v instanceof String) {
                ret = Long.parseLong((String) v);
            } else if (v instanceof Double) {
                ret = ((Double) v).longValue();
            }
            //here conversion error will be thrown
        }

        return ret;
    }

    public Long getLong(String key){
        Object v = get(key);
        return v!=null? (v instanceof Integer?((Integer)v).longValue():(Long)v):null;
    }

    public Boolean getBoolean(String key){
        return (Boolean) get(key);
    }

    public Double getDouble(String key){
        return (Double) get(key);
    }

    public Json getJson(String key){
        return (Json) get(key);
    }

    //This returns a array as a list
    public List<Object> getArray(String key){
        Object v = get(key);
        return v!=null?(List.class.isAssignableFrom(v.getClass())? (List<Object>)v:null):null;
    }

    public Object put(String key, Object value) {
        return super.put(key, (value instanceof Json && ((Json) value).isArray())?((Json) value).getArray():value);
    }

    public Json add(Object o){
        if(array == null) throw new IllegalStateException("not a Json array, it must be created with new Json(true)");
        array.add(o);
        return this;
    }

    public Json add(Collection o){
        if(array == null) throw new IllegalStateException("not a Json array, it must be created with new Json(true)");
        array.addAll(o);
        return this;
    }

    public Object get(int i){
        if(array == null) throw new IllegalStateException("not a Json array, it must be created with new Json(true)");
        return array.get(i);
    }

    public Iterator<Object> iterator(){
        if(array == null) throw new IllegalStateException("not a Json array, it must be created with new Json(true)");
        return array.iterator();
    }

    public List<Object> getArray() {
        return array;
    }

    public void setArray(List<Object> array) {
        this.array = array;
    }


    public static String jsonStr(Object ...kv){
        if(kv.length == 0) return "{}";
        if(kv.length % 2 != 0) throw new IllegalArgumentException("argument must be key/value pairs");
        StringBuilder ret = new StringBuilder("{\"");
        boolean st=false;
        for(int i=0; i < kv.length; i++) {
            if (!(kv[i] instanceof String)) throw new IllegalArgumentException("keys must be strings");
            if (st) ret.append(",\"");
            ret.append(kv[i++]).append("\":");
            if(kv[i] == null) {
                ret.append(kv[i]);
            }else if (kv[i] instanceof String){
                ret.append("\"").append(kv[i]).append("\"");
            }else if(kv[i] instanceof Number || kv[i] instanceof Boolean){ //(!Float.isNaN(f) && !Float.isInfinite(f)))
                ret.append(String.valueOf(kv[i]));
            }else {
                ret.append(kv[i].toString());
            }
            st=true;
        }
        ret.append("}");
        return ret.toString();
    }

    public static String escape(String str){
        return str.replace("\"", "\\\"");
    }


    public String _toJson() throws IOException {
        StringWriter sw = new StringWriter();
        try (JsonGenerator generator = factory.createGenerator(sw)) {
            if(array != null) toJsonArray(generator, array);
            else  toJson(generator, this);
            generator.close();
        }
        return sw.toString();
    }

    public String toJson() throws IOException {
        Encoder jb = new Encoder().startEncode(array != null, array == null);
        toJson(jb);
        return jb.endEncode(array == null);
    }

    public void toJson(Encoder jb) throws IOException {
        if(array != null) jb.toJson(array);
        else  jb.toJson(this);
    }

    public static <T> String toJson(Map<String, T> mp) throws IOException {
        Encoder jb = new Encoder().startEncode();
        for(Map.Entry<String, T> e: mp.entrySet()){
            jb.toJson(e.getKey(), (T)e.getValue());
        }
        return jb.endEncode();
    }

    public static <T> String toJson(List<T> lst) throws IOException {
        Encoder jb = new Encoder().startEncode(true, true);
        for (Object o: lst) {
            jb.toJson(o);
        }
        return jb.endEncode();
    }

    void toJson(JsonGenerator generator, Json js) throws IOException {
        generator.writeStartObject();
        for (Map.Entry<String, Object> e : js.entrySet()) {
            if(e.getValue() instanceof Json){
                generator.writeFieldName(e.getKey());
                toJson(generator, (Json)e.getValue());
                continue;
            }else if(e.getValue() instanceof List){
                generator.writeFieldName(e.getKey());
                toJsonArray(generator, (List) e.getValue());
                continue;
            }
            generator.writeObjectField(e.getKey(), e.getValue());
        }
        generator.writeEndObject();
    }

    void toJsonArray(JsonGenerator generator, List<Object> lst) throws IOException {
        generator.writeStartArray();
        for (Object o : lst) {
            if(o instanceof Json){
                toJson(generator, (Json)o);
            }else
                generator.writeObject(o);
        }
        generator.writeEndArray();
    }

	public static Json fromJson(byte [] json) throws IOException {
        Json top = null;

        try (JsonParser jp = factory.createParser(json)) {
            top = parse(jp, null);
            jp.close();
        }

        return top;
    }
    public static Json fromJson(String json) throws IOException {
		Json top = null;

        try (JsonParser jp = factory.createParser(json)) {
            top = parse(jp, null);
            jp.close();
        }

        return top;
    }

    public static Json fromJson(InputStream json) throws IOException {
        Json top = null;
        try (JsonParser jp = factory.createParser(json)){
            top = parse(jp, null);
            jp.close();
        }
        return top;
    }

    public static Json fromJsonFile(String file) throws IOException {
        InputStream in = ClassLoader.getSystemResourceAsStream(file);
        if (in == null) {
            in = Json.class.getClassLoader().getResourceAsStream(file);
            if (in == null) throw new IllegalArgumentException("File [" + file + "] not found!");
        }
        return fromJson(in);
    }


    static Json parse(JsonParser jp, Json cur) throws IOException {
        String fieldName = null;

        while (!jp.isClosed()) {
            JsonToken jsonToken = jp.nextToken();
        /*    if(start && (jsonToken != JsonToken.START_OBJECT && jsonToken != JsonToken.START_ARRAY))
                throw new IllegalArgumentException("Invalid json. must have a start tag");*/
            if(jsonToken == null) break;

            switch (jsonToken) {
                case START_OBJECT:
                    Json prev = cur;
                    cur = parse(jp, new Json());
                    if(fieldName != null && prev != null) prev.put(fieldName, cur);
                    cur = prev != null? prev: cur;
                    break;
                case END_OBJECT: return cur;
                case START_ARRAY:
                    //TODO: rethink if we should use Arraylist here or a plain object array - perf?
                    List<Object> lst = parseArray(jp, new ArrayList<>());
                    if(fieldName != null) cur.put(fieldName, lst);
                    else if(cur == null) {
                        cur=new Json();
                        cur.array = lst;
                    }
                    break;
                case END_ARRAY: break;
                case FIELD_NAME: fieldName = jp.getCurrentName(); break;
                case VALUE_STRING: cur.put(fieldName, jp.getValueAsString());break;
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT: cur.put(fieldName, jp.getNumberValue());break;
                case VALUE_TRUE:
                case VALUE_FALSE: cur.put(fieldName, jp.getBooleanValue());break;
                case VALUE_NULL: cur.put(fieldName, null);break;
                default:
                    throw new IOException("unexpected json token: "+jsonToken);
            }
        }
        if(cur == null) throw new IllegalArgumentException("Invalid json");
        return cur;
    }

    static List<Object> parseArray(JsonParser jp, List<Object> lst) throws IOException {
        while (!jp.isClosed()){
            JsonToken jsonToken = jp.nextToken();
            switch (jsonToken) {
                case START_OBJECT:
                    lst.add(parse(jp, new Json()));
                    break;
                case END_OBJECT: break;
                case START_ARRAY: lst.add(parseArray(jp, new ArrayList<>())); break;
                case END_ARRAY: return lst;
                case VALUE_STRING: lst.add(jp.getValueAsString());break;
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT: lst.add(jp.getNumberValue());break;
                case VALUE_TRUE:
                case VALUE_FALSE: lst.add(jp.getBooleanValue());break;
                case VALUE_NULL: lst.add(null);break;
            }
        }
        return lst;
    }


    //find keys from top to bottom, listed in an array. e.g. a,b,c from {"a":{"b":{"c":"", ...}, ...}, ...}
    public Json findKeys(String ... key){
        if(key == null) return this;
        Json js = this;
        if(isArray()){
            //FIXME
            Json ret = new Json(true);
            for(Object j: getArray()){
                Json jj = (Json)j;
                for (String k : key) {
                    jj = (Json) jj.get(k);
                    if (jj == null) continue;
                }
                ret.add(jj);
            }
        }else {
        for(String k:key) {
            js = (Json) js.get(k);
            if(js == null) return null;
        }
        }
        return js;
    }

    public Object clone(Json src) {
        if (src == null || !(src instanceof Map)) {
            return null;
        }

        Json ret = new Json();
        src.forEach((key, value) -> ret.put(key, value));

        return ret;
    }

    @Override
    public String toString() {
        try {
            return toJson();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Override
    public boolean isEmpty() {
        return isArray()? array.size() == 0: super.isEmpty();
    }

    @Override
    public int size() {
        if(isArray()) return array.size();
        return super.size();
    }



    /*--- generator ---*/

    public static class Encoder{

        final byte obj=1;
        final byte arr=2;
        final byte fld=3;

        StringWriter sw;
        JsonGenerator builder;
        boolean startedArray = false;
        Object ref = null;
        byte [] lwo=new byte[100];
        int idx=0;

        void push(byte itm){
            if(idx != 0 && lwo[idx-1] == fld) return;
            lwo[idx++]=itm;
        }

        void pop(){
            if(idx != 0) lwo[--idx]=0;
        }

        void popitm(int itm){
            if(idx != 0 && lwo[idx - 1]==itm) lwo[--idx]=0;
        }

        boolean ispeek(int itm){
            return idx != 0 && lwo[idx - 1] == itm;
        }

        boolean writeStartIfReq() throws IOException {
            if(!ispeek(obj)) {
                builder.writeStartObject();
                push(obj);
                return true;
            }
            return false;
        }

        void writeEndIfReq(boolean wrote) throws IOException {
            if(wrote) {
                builder.writeEndObject();
                popitm(fld);
                popitm(obj);
            }
        }

        boolean writeStartArrayIfReq() throws IOException {
            if(!ispeek(obj)) {
                builder.writeStartArray();
                push(arr);
                return true;
            }
            return false;
        }

        void writeEndArrayIfReq(boolean wrote) throws IOException {
            if(wrote) {
                builder.writeEndArray();
                popitm(arr);
            }
        }

        public Encoder startEncode() {
            return startEncode(false, true);
        }

        public Encoder startEncode(boolean array, boolean start){
            sw= new StringWriter();
            try {
                builder = factory.createGenerator(sw);
                if(start) {
                    startedArray = array;
                    if (!startedArray) {
                        builder.writeStartObject();
                        push(obj);
                    }else {
                        builder.writeStartArray();
                        push(arr);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }

        public String endEncode() throws IOException{
            return endEncode(true);
        }

        public String endEncode(boolean end) throws IOException{

            if(end) {
                if (!startedArray)
                    builder.writeEndObject();
                else builder.writeEndArray();
                pop();
            }

            builder.close();
            sw.flush();
            return sw.toString();
        }

        public Encoder toJson(String name, String value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeStringField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, int value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            builder.writeNumberField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, Integer value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeNumberField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, long value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            builder.writeNumberField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, Long value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeNumberField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, BigDecimal value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeNumberField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, float value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            builder.writeNumberField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, Float value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeNumberField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, double value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            builder.writeNumberField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, Double value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeNumberField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, boolean value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            builder.writeBooleanField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, Boolean value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeBooleanField(name, value);
            push(fld);
            return this;
        }

        public Encoder toJson(String name, Calendar value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            //this could loose some date related data,  like time zone & locale.
            builder.writeObjectField(name, DatatypeConverter.printDateTime(value));
            push(fld);
            return this;
        }

        public Encoder toJson(String name, org.apache.commons.lang.enums.Enum value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeObjectField(name, value.getName());
            push(fld);
            return this;
        }

        public Encoder toJson(Json value) throws IOException {
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }

            if(value.isArray()) {
                toJson(value.getArray());
            }else {
                boolean wrote = writeStartIfReq();
                for(Map.Entry<String, Object> e: value.entrySet()){
                    toJson(e.getKey(), e.getValue());
                }
                writeEndIfReq(wrote);
            }

            return this;
        }

        //Exceptional behavior, not writing start/end object tags
        public <T> Encoder toJson(Map<String, T> value) throws IOException {
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }

            boolean wrote = writeStartIfReq();
            for(Map.Entry<String, T> e: value.entrySet()){
                toJson(e.getKey(), (T)e.getValue());
            }
            writeEndIfReq(wrote);
            return this;
        }

        public <T> Encoder toJson(List<T> lst) throws IOException {
            if(lst == null) return this;
            boolean wrote = writeStartArrayIfReq();
            for(T v:lst){
                toJson(v);
            }
            writeEndArrayIfReq(wrote);
            return this;
        }

        public <T> Encoder toJson(Collection<T> lst) throws IOException {
            if(lst == null) return this;
            boolean wrote = writeStartArrayIfReq();
            for(T v:lst){
                toJson(v);
            }
            writeEndArrayIfReq(wrote);
            return this;
        }

        /*public Encoder toJson(String name, Map<String, Entity> value) throws IOException {
                    if(value == null) {
                        //builder.writeNullField(name);
                        return this;
                    }
                    builder.writeFieldName(name);
                    boolean wrote = writeStartArrayIfReq();
                    for(Map.Entry<String, Entity> e:value.entrySet()){
                        boolean wrote2 = writeStartIfReq();
                        e.getValue().toJson(this);
                        writeEndIfReq(wrote2);
                    }
                    writeEndArrayIfReq(wrote);
                    return this;
                }
            */
        public Encoder toJson(String name, Json value) throws IOException {
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeFieldName(name);
            push(fld);
    //        boolean wrote = writeStartIfReq();
            toJson(value);
    //        writeEndIfReq(wrote);

            return this;
        }

        public Encoder toJson(String name, Collection value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeFieldName(name);
            push(fld);
            boolean wrote = writeStartArrayIfReq();
            for(Object lid:value){
                toJson(lid);
            }
            writeEndArrayIfReq(wrote);

            return this;
        }

        public <T> Encoder toJsonArray(String name, List<T> lst) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(lst == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeFieldName(name);
            push(fld);
            return toJson(lst);
        }

        public Encoder toJsonArray(String name, Map<String, String> value) throws IOException {
            if(name == null || name.isEmpty()) throw new IllegalArgumentException();
            if(value == null) {
                //builder.writeNullField(name);
                return this;
            }
            builder.writeFieldName(name);
            push(fld);
            boolean wrote = writeStartArrayIfReq();
            for(Map.Entry<String, String> e:value.entrySet()){
                builder.writeStringField(e.getKey(), e.getValue());
            }
            writeEndArrayIfReq(wrote);
            return this;
        }

        public Encoder toJson(String name, Object value) throws IOException {
            //TODO: is it ok to allow empty string as name? but it's a valid json
            if(name == null) throw new IllegalArgumentException("name cannot be null for value "+value);
            if(value == null) {
    //            builder.writeNullField(name);
                return this;
            }

            if(value instanceof org.apache.commons.lang.enums.Enum) return toJson(name, (org.apache.commons.lang.enums.Enum) value);
    //        else if(value instanceof List<?>) toJson(name, (List<?>) value);
            else if(value instanceof Collection<?>) toJson(name, (Collection<?>)value);
            else if(value instanceof Json) {
                //avoid repeated self reference which cause stack overflow
                //FIXME:can cause unexpected.
                if(ref == null) ref = value;
                else if(ref == value) return this;
                toJson(name, (Json) value);
                ref = null;
            }else {
                try {
                    Method m = value.getClass().getMethod("toJson", Encoder.class);
                    if (m != null) {
                        builder.writeFieldName(name);
                        push(fld);
                        boolean wrote = writeStartIfReq();
                        m.invoke(value, this);
                        writeEndIfReq(wrote);
                    } else {
                        builder.writeObjectField(name, value);
                        push(fld);
                    }
                } catch (NoSuchMethodException e) {
                    builder.writeObjectField(name, value);
                    push(fld);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new IOException(e);
                }
            }
            return this;
        }

        public Encoder toJson(Object value) throws IOException {
            if(value == null) {
                //builder.writeNull();
                return this;
            }

            if(value instanceof org.apache.commons.lang.enums.Enum)
                builder.writeObject(((org.apache.commons.lang.enums.Enum) value).getName());
    //        else if(value instanceof List) toJson((List<?>) value);
            else if(value instanceof Collection<?>) toJson((Collection<?>)value);
            else if(value instanceof Json) toJson((Json)value);
            else {
                //TODO: assume this is not a collection/array
                try {
                    Method m = value.getClass().getMethod("toJson", Encoder.class);
                    if (m != null) {
                        boolean wrote = writeStartIfReq();
                        m.invoke(value, this);
                        writeEndIfReq(wrote);
                    } else builder.writeObject(value);
                } catch (NoSuchMethodException e) {
                    builder.writeObject(value);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new IOException(e);
                }

            }
            return this;
        }
    }


    /*---- parser ---*/

    public static class Decoder {

        JsonParser parser;
        Object rawjson;
        JsonToken currToken;

        public Decoder parse(String json) throws IOException {
            parser = factory.createParser(json);
            rawjson = json;
            return this;
        }

        public Decoder parse(InputStream json) throws IOException {
            parser = factory.createParser(json);
            rawjson = json;
            return this;
        }

        public void close() throws IOException {
            parser.close();
        }

        public Decoder next() throws IOException {
            currToken = parser.nextToken();
            return this;
        }

        public boolean isClosed() {
            return parser.isClosed();
        }

        public boolean isEOS() {
            return currToken == null;
        }

        public boolean isNextStartObject() throws IOException {
            return parser.nextToken() == JsonToken.START_OBJECT;
        }

        public boolean isNextEndObject() throws IOException {
            return parser.nextToken() == JsonToken.END_OBJECT;
        }

        public boolean isNextStartArray() throws IOException {
            return parser.nextToken() == JsonToken.START_ARRAY;
        }

        public boolean isNextEndArray() throws IOException {
            return parser.nextToken() == JsonToken.END_ARRAY;
        }

        public boolean isStartObject() throws IOException {
            return parser.currentToken() == JsonToken.START_OBJECT;
        }

        public boolean isEndObject() throws IOException {
            return parser.currentToken() == JsonToken.END_OBJECT;
        }

        public boolean isStartArray() throws IOException {
            return parser.currentToken() == JsonToken.START_ARRAY;
        }

        public boolean isEndArray() throws IOException {
            return parser.currentToken() == JsonToken.END_ARRAY;
        }

        public boolean isField() throws IOException {
            return parser.currentToken() == JsonToken.END_ARRAY;
        }

        public String fieldName() throws IOException {
            return parser.getCurrentName();
        }

        public String fieldValueAsString() throws IOException {
            return parser.getValueAsString();
        }

        public Object fieldValue() throws IOException {
            JsonToken jsonToken = parser.getCurrentToken();
            switch (jsonToken) {
                case VALUE_STRING:
                    return parser.getValueAsString();
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                    return parser.getNumberValue();
                case VALUE_TRUE:
                case VALUE_FALSE:
                    return parser.getValueAsBoolean();
                case VALUE_NULL:
                    return null;
            }
            return null;
        }

        public long getCharLocation() {
            JsonLocation loc = parser.getCurrentLocation();
            return loc.getCharOffset();
        }

        public  int[] getLocation() {
            JsonLocation loc = parser.getCurrentLocation();

            return new int[]{loc.getLineNr(), loc.getColumnNr()};
        }

        public String rawJson() throws IOException {
            if (rawjson instanceof String) return (String) rawjson;
            if (rawjson instanceof InputStream) {
                byte[] buf = new byte[1024];
                InputStream in = (InputStream) rawjson;
                StringBuilder b = new StringBuilder();
                while (in.read(buf) > 0) {
                    b.append(buf);
                }
                return b.toString();
            }
            return "";
        }
    }
}
