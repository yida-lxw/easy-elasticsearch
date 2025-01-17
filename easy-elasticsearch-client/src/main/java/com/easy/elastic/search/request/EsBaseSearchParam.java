package com.easy.elastic.search.request;

import java.io.Serializable;
import java.util.Map;

/**
 * @author yida
 * @description ES查询用户输入参数基类
 * @date 2025-01-17 10:47:11
 * @return {@link null}
 */
public abstract class EsBaseSearchParam implements Serializable {
    /**
     * 动态字段；例如搜索字段：onlineDate的时间范围 key=onlineDate,value={"searchType":"esRange","start":20240701,"end":20240730,"includeUpper":true,"includeLower":true}
     */
    protected Map<String, DynamicSearchField> dynamicFieldsMap;

    public Map<String, DynamicSearchField> getDynamicFieldsMap() {
        return dynamicFieldsMap;
    }

    public void setDynamicFieldsMap(Map<String, DynamicSearchField> dynamicFieldsMap) {
        this.dynamicFieldsMap = dynamicFieldsMap;
    }

    public static abstract class EsBaseSearchParamBuilder {
        private Map<String, DynamicSearchField> dynamicFieldsMap;

        public Map<String, DynamicSearchField> getDynamicFieldsMap() {
            return dynamicFieldsMap;
        }

        public EsBaseSearchParamBuilder setDynamicFieldsMap(Map<String, DynamicSearchField> dynamicFieldsMap) {
            this.dynamicFieldsMap = dynamicFieldsMap;
            return this;
        }

        protected abstract <E extends EsBaseSearchParam> E build();
    }
}
