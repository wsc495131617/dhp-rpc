package org.dhp.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Jackson序列化和反序列化工具类
 * @author zhangcb
 */
@Slf4j
public class JacksonUtil {

    private static ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String bean2Json(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    public static byte[] bean2JsonBytes(Object obj) {
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    public static <T> T json2Bean(String jsonStr, Class<T> objClass) {
        try {
            return mapper.readValue(jsonStr, objClass);
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    public static <T> T bytes2Bean(byte[] bytes, Class<T> objClass) {
        try {
            return mapper.readValue(bytes, objClass);
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

}