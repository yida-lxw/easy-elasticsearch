package cn.yzw.jc2.common.search.request;

import java.io.Serializable;
import java.util.Map;

public class EsSearchBase implements Serializable {
    /**
     * 动态字段；例如搜索字段：onlineDate的时间范围 key=onlineDate,value={"searchType":"esRange","start":20240701,"end":20240730,"includeUpper":true,"includeLower":true}
     */
    private Map<String, DynamicSearchField> dynamicFieldsMap;

    public Map<String, DynamicSearchField> getDynamicFieldsMap() {
        return dynamicFieldsMap;
    }

    public void setDynamicFieldsMap(Map<String, DynamicSearchField> dynamicFieldsMap) {
        this.dynamicFieldsMap = dynamicFieldsMap;
    }
}
