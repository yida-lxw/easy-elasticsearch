package cn.yzw.jc2.json.fastJson;

import java.lang.reflect.Type;
import java.nio.charset.CharsetDecoder;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;

/**
 * @Description: 支持多种类型的日期格式处理
 * @Author: lbl
 * @Date: 2023/4/7
 **/
public class JcFastJsonUtils {
    private static final SerializerFeature[] serializerFeatures;
    private static final Feature[] deSerializerFeatures;
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final SerializeConfig mapping;

    private JcFastJsonUtils() {
    }

    public static String toJSONString(Object obj) {
        return toJSONString(obj, (String)null);
    }

    public static String toJSONString(Object obj, String dateFormat) {
        return StringUtils.isBlank(dateFormat) ? JSON.toJSONString(obj, mapping, serializerFeatures) : JSON.toJSONStringWithDateFormat(obj, dateFormat, serializerFeatures);
    }

    public static byte[] toJSONStringBytes(Object obj) {
        return toJSONStringBytes(obj, (String)null);
    }

    public static byte[] toJSONStringBytes(Object obj, String dateFormat) {
        if (!StringUtils.isBlank(dateFormat)) {
            mapping.put(Date.class, new SimpleDateFormatSerializer(dateFormat));
        }

        return JSON.toJSONBytes(obj, mapping, serializerFeatures);
    }


    public static <T> T parseObject(String json, Type type) {
        return JSON.parseObject(json, type, deSerializerFeatures);
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
            ParserConfig config = new ParserConfig();
            config.putDeserializer(Date.class, new FastDateFormatDeserializer());
            return JSON.parseObject(json, clazz, config, JSON.DEFAULT_PARSER_FEATURE, deSerializerFeatures);

    }
    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        ParserConfig config = new ParserConfig();
        config.putDeserializer(Date.class, new FastDateFormatDeserializer());
        return JSON.parseArray(json, clazz, config);

    }
    public static <T> T fromJSONString(String json, TypeReference<T> ref) {
        return JSON.parseObject(json, ref, deSerializerFeatures);
    }


    public static <T> T fromJSONString(byte[] bytes, Class<T> clazz) {
        return JSON.parseObject(bytes, clazz, deSerializerFeatures);
    }



    /**
     * json添加新的元素
     * @param bytes
     * @param off
     * @param len
     * @param charsetDecoder
     * @param clazz
     * @return
     * @param <T>
     */
    public static <T> T fromJSONString(byte[] bytes, int off, int len, CharsetDecoder charsetDecoder, Class<T> clazz) {
        return JSON.parseObject(bytes, off, len, charsetDecoder, clazz, deSerializerFeatures);
    }

    public static String addEntry(String json, String key, String value, boolean addDelimiter) {
        StringBuilder buff = new StringBuilder();
        buff.append("{").append("\"").append(key).append("\"").append(":").append("\"").append(value).append("\"");
        if (addDelimiter) {
            buff.append(",");
        }

        buff.append(json.substring(1));
        return buff.toString();
    }

    static {
        serializerFeatures = new SerializerFeature[]{SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullListAsEmpty, SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.SortField, SerializerFeature.SkipTransientField};
        deSerializerFeatures = new Feature[]{Feature.AllowISO8601DateFormat,Feature.AllowUnQuotedFieldNames, Feature.AllowSingleQuotes, Feature.InternFieldNames, Feature.AllowArbitraryCommas, Feature.IgnoreNotMatch};
        mapping = new SerializeConfig();
        mapping.put(Date.class, new SimpleDateFormatSerializer(DEFAULT_DATE_FORMAT));
    }
}
