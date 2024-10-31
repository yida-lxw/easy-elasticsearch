package com.easy.elastic.search.request;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class DynamicSearchField implements Serializable {

    /**
     * 搜索类型：esEquals:等于，esIn:多值精确匹配，esLike:模糊搜索，esRange:范围匹配，esNotLike:模糊搜索不包含
     */
    private String       searchType;

    /**
     * nested path or field
     */
    private String       nested;

    /**
     * EsNestedTypeEnum 默认path
     */
    private String       nestedType;

    /**
     * 范围查询起
     */
    private Object       startValue;

    /**
     * 范围查询止
     */
    private Object       endValue;

    /**
     * 包含上界
     */
    private Boolean      includeUpper;

    /**
     * 包含下界
     */
    private Boolean      includeLower;

    /**
     * 搜索值,用于=
     */
    private Object       value;

    /**
     * 搜索多值,用于in
     */
    private List<Object> valueList;

}
