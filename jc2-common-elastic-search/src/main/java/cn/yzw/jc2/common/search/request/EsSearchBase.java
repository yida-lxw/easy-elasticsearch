package cn.yzw.jc2.common.search.request;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@SuperBuilder
public class EsSearchBase implements Serializable {
    /**
     * 动态字段；例如搜索字段：onlineDate的时间范围 key=onlineDate,value={"searchType":"esRange","start":20240701,"end":20240730,"includeUpper":true,"includeLower":true}
     */
    private Map<String, DynamicSearchField> dynamicFieldsMap;
}
