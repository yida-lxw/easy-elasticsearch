package cn.yzw.jc2.common.util.json.jackson;

import java.lang.reflect.Type;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * JSON工具类（使用jackson封装）
 *
 */
public class Jc2JacksonUtil {

    /**
     * 日志
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Jc2JacksonUtil.class);

    /**
     * 转换器
     */
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        // 允许没有引号的字段名
        MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 允许单引号字段名
        MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        // 自动给字段名加上引号
        MAPPER.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
        // 时间默认以时间戳格式写
        // MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 设置时间转换所使用的默认时区
        MAPPER.setTimeZone(TimeZone.getDefault());
        // null不生成到json字符串中
        MAPPER.setSerializationInclusion(Include.NON_NULL);

        // 全局日期反序列化配置
        SimpleModule module = new SimpleModule();
        module.addDeserializer(java.util.Date.class, new DateDeserializer());
        module.addDeserializer(java.sql.Date.class, new DateDeserializers.SqlDateDeserializer());
        module.addDeserializer(java.sql.Timestamp.class, new DateDeserializers.TimestampDeserializer());
        MAPPER.registerModule(module);
    }

    /**
     * 构造方法
     */
    private Jc2JacksonUtil() {

    }

    /**
     * 将JSON字符串内容转换为集合类型
     *
     * @param <T> 泛型对象
     * @param content JSON字符串内容
     * @param clazz List泛型中对象类型
     * @return List集合
     */
    public static <T> List<T> parseArray(String content, Class<T> clazz) {
        try {
            CollectionType javaType = MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
            return MAPPER.readValue(content, javaType);
        } catch (Exception e) {
            LOGGER.error("JSON反序列化失败：" + e.getMessage(), e);
            throw new RuntimeException("JSON反序列化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将JSON字符串内容转换为对象类型
     *
     * @param content JSON字符串内容
     * @param valueType 值类型
     * @param <T> 对象类型
     * @return 将JSON字符串内容转换为对象类型
     */
    public static <T> T parseObject(String content, Class<T> valueType) {
        try {
            return MAPPER.readValue(content, valueType);
        } catch (Exception e) {
            LOGGER.error("JSON反序列化失败：" + e.getMessage(), e);
            throw new RuntimeException("JSON反序列化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将JSON字符串内容转换为泛型所对应的类型
     *
     * @param content JSON字符串内容
     * @param genericType 泛型类型
     * @param <T> 对象类型
     * @return 将JSON字符串内容转换为对象类型
     */
    public static <T> T parseObject(String content, Type genericType) {
        try {
            return MAPPER.readValue(content, TypeFactory.defaultInstance().constructType(genericType));
        } catch (Exception e) {
            LOGGER.error("JSON反序列化失败：" + e.getMessage(), e);
            throw new RuntimeException("JSON反序列化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将JSON字符串内容转换为对象类型-对象泛型支持
     *
     * @param <T> 泛型类型
     * @param content JSON字符串内容
     * @param valueTypeRef 泛型类型
     * @return 对象类型
     */
    public static <T> T parseObject(String content, TypeReference<T> valueTypeRef) {
        try {
            return MAPPER.readValue(content, valueTypeRef);
        } catch (Exception e) {
            LOGGER.error("JSON反序列化失败：" + e.getMessage(), e);
            throw new RuntimeException("JSON反序列化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将对象转换为JSON字符串
     *
     * @param object 对象
     * @return JSON字符串
     */
    public static String toJSONString(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            LOGGER.error("JSON序列化失败：" + e.getMessage(), e);
            throw new RuntimeException("JSON序列化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将来源对象转换为目标对象
     *
     * @param <T> 泛型对象
     * @param fromValue 对象值
     * @param toValueTypeRef 目标对象泛型
     * @return 目标对象
     */
    public static <T> T convertObject(Object fromValue, TypeReference<T> toValueTypeRef) {
        return MAPPER.convertValue(fromValue, toValueTypeRef);
    }

    /**
     * 将来源对象转换为目标对象
     *
     * @param <T> 泛型对象
     * @param fromValue 对象值
     * @param toValueType 目标对象class类型
     * @return 目标对象
     */
    public static <T> T convertObject(Object fromValue, Class<T> toValueType) {
        return MAPPER.convertValue(fromValue, toValueType);
    }

    /**
     * 获取ObjectMapper对象
     *
     * @return 获取ObjectMapper对象
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

}
