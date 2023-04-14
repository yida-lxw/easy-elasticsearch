package cn.yzw.jc2.json.fastJson;

import java.lang.reflect.Type;
import java.util.Date;

import cn.yzw.jc2.json.DatePatternUtil;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.serializer.DateCodec;


public class FastDateFormatDeserializer extends DateCodec {


    public FastDateFormatDeserializer() {
    }

    public Date cast(DefaultJSONParser parser, Type clazz, Object fieldName, Object val) {
        return DatePatternUtil.getDate(val);
    }


}