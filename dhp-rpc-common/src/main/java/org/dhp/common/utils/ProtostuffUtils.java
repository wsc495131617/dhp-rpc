package org.dhp.common.utils;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProtostuffUtils {

    static Map<Class, RuntimeSchema> cacheSchema = new ConcurrentHashMap<>();

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
        RuntimeSchema<T> schema;
        T newInstance;
        try {
            schema = RuntimeSchema.createFrom(typeClass);
            newInstance = schema.newMessage();
            ProtostuffIOUtil.mergeFrom(source, newInstance, schema);
        } catch (Exception e) {
            throw new RuntimeException("deserialize exception");
        }

        return newInstance;
    }
    
}