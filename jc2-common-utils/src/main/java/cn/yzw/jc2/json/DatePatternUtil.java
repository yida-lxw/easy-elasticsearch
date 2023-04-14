package cn.yzw.jc2.json;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSONException;

import cn.hutool.core.date.DateUtil;

/**
 * 日期正则匹配工具类
 *
 */
public class DatePatternUtil {

    /**
     * 构造方法
     */
    private DatePatternUtil() {

    }

    /** 格式化 */
    public static final List<Pattern>        patternList = new ArrayList<Pattern>(5);

    /** 格式匹配模式 */
    private static final Pattern             PATTERN1    = Pattern.compile("\\d{4}");

    /** 格式匹配模式 */
    private static final Pattern             PATTERN2    = Pattern.compile("\\d{4}-\\d{1,2}");

    /** 格式匹配模式 */
    private static final Pattern             PATTERN3    = Pattern.compile("(\\d{4}\\-\\d{1,2}\\-\\d{1,2})");

    /** 格式匹配模式 */
    private static final Pattern             PATTERN4    = Pattern
        .compile("(\\d{4}\\-\\d{1,2}\\-\\d{1,2} \\d{1,2}:\\d{1,2})");

    /** 格式匹配模式 */
    private static final Pattern             PATTERN5    = Pattern
        .compile("\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}");

    /** 格式匹配模式 */
    private static final Pattern             PATTERN6    = Pattern
        .compile("\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d+");

    /** 格式匹配模式 */
    private static final Pattern             PATTERN7    = Pattern.compile("\\d{4}/\\d{1,2}/\\d{1,2}");

    /** 格式匹配模式 */
    private static final Pattern             PATTERN8    = Pattern
        .compile("\\w{3}\\s\\w{3}\\s\\d{1,2}\\s\\d{4}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\sGMT\\+0800");

    /** 格式匹配模式 */
    private static final Pattern             PATTERN9    = Pattern.compile("\\d{4}\\d{1,2}");

    /** 格式匹配模式 */
    private static final Pattern             PATTERN10   = Pattern.compile("(\\d{4}\\d{1,2}\\d{1,2})");

    /**
     * 支持格式
     * 20221123T122025
     * 20221123T122025Z
     * 2022-11-23T12:20:25.000
     * 2022-11-23T12:20:25.000Z
     * 2022-11-23T12:20:25
     */
    private static final Pattern             PATTERN11   = Pattern.compile(
        "^([\\+-]?\\d{4}(?!\\d{2}\\b))((-?)((0[1-9]|1[0-2])(\\3([12]\\d|0[1-9]|3[01]))?|W([0-4]\\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]\\d|6[1-6])))([T\\s]((([01]\\d|2[0-3])((:?)[0-5]\\d)?|24\\:?00)([\\.,]\\d+(?!:))?)?(\\17[0-5]\\d([\\.,]\\d+)?)?([zZ]|([\\+-])([01]\\d|2[0-3]):?([0-5]\\d)?)?)?)?(Z?)$");

    /** 格式化 */
    public static final Map<Pattern, String> patternMap  = new HashMap<Pattern, String>();

    static {
        patternMap.put(PATTERN1, "yyyy");
        patternMap.put(PATTERN2, "yyyy-MM");
        patternMap.put(PATTERN3, "yyyy-MM-dd");
        patternMap.put(PATTERN4, "yyyy-MM-dd HH:mm");
        patternMap.put(PATTERN5, "yyyy-MM-dd HH:mm:ss");
        patternMap.put(PATTERN6, "yyyy-MM-dd HH:mm:ss.SSS");
        patternMap.put(PATTERN7, "yyyy/MM/dd");
        patternMap.put(PATTERN8, "EEE MMM dd yyyy HH:mm:ss 'GMT+0800'");
        patternMap.put(PATTERN9, "yyyyMM");
        patternMap.put(PATTERN10, "yyyyMMdd");
        patternMap.put(PATTERN11, "yyyy-MM-ddTHH:mm:ss.SSS");

        // 添加pattern
        patternList.add(PATTERN1);
        patternList.add(PATTERN2);
        patternList.add(PATTERN3);
        patternList.add(PATTERN4);
        patternList.add(PATTERN5);
        patternList.add(PATTERN6);
        patternList.add(PATTERN7);
        patternList.add(PATTERN8);
        patternList.add(PATTERN9);
        patternList.add(PATTERN10);
        patternList.add(PATTERN11);
    }

    /**
     * 获取需要反序列化为正确格式的日期
     *
     * @param strDateValue 字符串类型的日期值
     * @return Date
     */
    public static Date getPatternDate(String strDateValue) {
        String value = strDateValue;
        if (value == null || "".equals(value.trim()) || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        // 解决字符串被自动转码导致的问题，在此将转码后的字符串还原。
        if (value.indexOf('%') >= 0) {
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("不支持的时间格式:" + value);
            }
        }

        String format = getMatchFormat(value);
        if (format == null) {
            // 如果以上8种时间格式都无法匹配，校验是否是时间戳格式，如果是就直接转换为Date，否则直接抛出异常
            Matcher matcher = Pattern.compile("[-]?\\d+").matcher(value);
            boolean isMatch = matcher.matches();
            if (isMatch) {
                return new Date(Long.valueOf(value));
            }
            throw new IllegalArgumentException("不支持的时间格式:" + value);
        }

        if (format.indexOf("GMT") > 0) {
            SimpleDateFormat objSimpleFormat = new SimpleDateFormat(format, Locale.US);
            try {
                return objSimpleFormat.parse(value);
            } catch (ParseException e) {
                throw new IllegalArgumentException("不支持的时间格式:" + value);
            }
        }
        if (format.indexOf("T") > 0) {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toFormatter();
            try {
                TemporalAccessor parse = formatter.parse(value);
                return DateUtil.date(parse);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("不支持的时间格式:" + value);
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.parse(value);
        } catch (ParseException e) {
            throw new IllegalArgumentException("不支持的时间格式:" + value);
        }
    }

    public static Date getDate(Object val) {
        if (val == null) {
            return null;
        } else if (val instanceof Calendar) {
            return ((Calendar) val).getTime();
        } else if (val instanceof Date) {
            return (Date) val;
        } else if (val instanceof Number) {
            long longValue = ((Number) val).longValue();
            return new Date(longValue);
        } else if (val instanceof String) {
            return getPatternDate((String) val);
        } else {
            throw new JSONException("parse error " + val);
        }

    }

    /**
     * 根据值获取合适的格式
     *
     *
     * @param value 数据
     * @return 格式
     */
    public static String getMatchFormat(final String value) {
        for (Iterator<Pattern> iterator = DatePatternUtil.patternList.iterator(); iterator.hasNext();) {
            Pattern pattern = iterator.next();
            Matcher matcher = pattern.matcher(value);
            boolean isMatch = matcher.matches();
            if (isMatch) {
                return DatePatternUtil.patternMap.get(pattern);
            }
        }
        return null;
    }

}
