package com.easy.elastic.search.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class JsonUtils {

    private static ObjectMapper objectMapper = null;

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = buildObjectMapper();
        }

        return objectMapper;
    }

    public static void setObjectMapper(ObjectMapper objectMapper) {
        JsonUtils.objectMapper = objectMapper;
    }

    /**
     * JSON反序列化
     *
     * @param json     json str
     * @param objClass object class
     * @param <T>      {@link T}
     * @return {@link T}
     */
    public static <T> T readAsObject(String json, Class<T> objClass) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return getObjectMapper().readValue(json, objClass);
        } catch (IOException e) {
            throw new RuntimeException("json to object valueType err:" + e.getMessage());
        }
    }

    public static Map<String, Object> readAsMap(String json) {
        return readAsMap(json, String.class, Object.class);
    }

    public static <K, V> Map<K, V> readAsMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            JavaType javaType = getObjectMapper().getTypeFactory().constructParametricType(Map.class, keyClass, valueClass);
            return getObjectMapper().readValue(json, javaType);
        } catch (IOException e) {
            throw new RuntimeException("json to object valueType err:" + e.getMessage());
        }
    }

    public static String writeAsJson(Object val) {
        if (val == null) {
            return null;
        }
        try {

            return getObjectMapper().writeValueAsString(val);
        } catch (Exception e) {
            throw new RuntimeException("object to json err:" + e.getMessage());
        }
    }

    public static <T> List<T> readAsList(String json, Class<T> targetClass) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            JavaType javaType = getObjectMapper().getTypeFactory().constructParametricType(List.class, targetClass);
            return getObjectMapper().readValue(json, javaType);
        } catch (IOException e) {
            throw new RuntimeException("json to list  valueType err:" + e.getMessage());
        }
    }

    public static <K, V> List<Map<K, V>> readAsListMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            JavaType mapType = getObjectMapper().getTypeFactory().constructParametricType(Map.class, keyClass, valueClass);
            JavaType javaType = getObjectMapper().getTypeFactory().constructParametricType(List.class, mapType);
            return getObjectMapper().readValue(json, javaType);
        } catch (IOException e) {
            throw new RuntimeException("json to list  valueType err:" + e.getMessage());
        }
    }

    public static <T> T readAsType(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json)) {
            return null;
        }

        try {
            return getObjectMapper().readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("json to javaType  valueType err:" + e.getMessage());
        }
    }

    public static ObjectMapper buildObjectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // serializer
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        // deserializer
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, false);

        // 默认忽略了相同类型的javaTimeModel的注册，所以需要先disable这条规则
        objectMapper.disable(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS);
        // 完成自定义javaTimeModel注册
        objectMapper.registerModule(javaTimeModule);
        // 重新开启忽略规则
        objectMapper.enable(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS);
        return objectMapper;
    }

}
