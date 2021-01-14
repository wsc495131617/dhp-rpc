package org.dhp.common.utils;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ProtostuffUtils {

    @Data
    static class ObjectWrapper {
        Object data;
    }

    static Map<Class, RuntimeSchema> cacheSchema = new ConcurrentHashMap<>();
    static {
        //允许List字段空的时候序列化null
        System.setProperty("protostuff.runtime.pojo_schema_on_collection_fields","true");
    }

    static RuntimeSchema getSchema(Class cls) {
        if (cacheSchema.containsKey(cls)) {
            return cacheSchema.get(cls);
        }
        RuntimeSchema schema = RuntimeSchema.createFrom(cls);
        cacheSchema.put(cls, schema);
        return schema;
    }

    public static <T> byte[] serialize(T source) {
        RuntimeSchema<T> schema = getSchema((Class<T>) source.getClass());
        return _serialize(source, schema);
    }

    public static <T> byte[] serialize(Class<T> typeClass, T source) {
        RuntimeSchema<T> schema = getSchema((Class<T>) typeClass);
        return _serialize(source, schema);
    }

    static <T> byte[] _serialize(T source, RuntimeSchema<T> schema) {
        if(source == null) {
            return EMPTY;
        }
        LinkedBuffer buffer = null;
        byte[] result;
        try {
            buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
            result = ProtostuffIOUtil.toByteArray(source, schema, buffer);
        } catch (Exception e) {
            throw new RuntimeException("serialize exception");
        } finally {
            if (buffer != null) {
                buffer.clear();
            }
        }

        return result;
    }

    public static <T> T deserialize(byte[] source, Class<T> typeClass) {
        if (source == null || source.length == 0) {
            try {
                return typeClass.newInstance();
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            }
            return null;
        }
        RuntimeSchema<T> schema;
        T newInstance;
//        try {
            schema = RuntimeSchema.createFrom(typeClass);
            newInstance = schema.newMessage();
            ProtostuffIOUtil.mergeFrom(source, newInstance, schema);
//        } catch (Exception e) {
//            throw new RuntimeException("deserialize exception");
//        }

        return newInstance;
    }
    
    static byte[] EMPTY = new byte[0];
    
    public static <T> byte[] serializeList(List<T> list) {
        if(list == null || list.isEmpty()){
            return EMPTY;
        }
        Schema<T> schema = (Schema<T>) RuntimeSchema.getSchema(list.get(0).getClass());
        LinkedBuffer buffer = LinkedBuffer.allocate(1024*1024);
        byte[] protostuff = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try{
            byteArrayOutputStream = new ByteArrayOutputStream();
            ProtostuffIOUtil.writeListTo(byteArrayOutputStream, list, schema, buffer);
            protostuff = byteArrayOutputStream.toByteArray();
        } catch (Exception e){
            log.error("List<{}> serializeList Failed", schema.typeClass());
            throw new RuntimeException("serializeList failed");
        }
        return protostuff;
    }
    
    public static <T> List<T> deserializeList(byte[] bytes, Class<T> itemTypeClass){
        if(bytes == null || bytes.length == 0){
            return null;
        }
        Schema<T> schema = RuntimeSchema.getSchema(itemTypeClass);
        List<T> result = null;
        try {
            result = ProtostuffIOUtil.parseListFrom(new ByteArrayInputStream(bytes), schema);
        } catch (IOException e){
            log.error("parse List<{}> failed", itemTypeClass);
            throw new RuntimeException("deserializeList failed");
        }
        return result;
    }
    
}