package com.easy.elastic.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 聚合查询返回 key 枚举
 */
@Getter
@AllArgsConstructor
public enum EsDistinctAggEnum {

    /**
     * 去重指定 key 值
     */
    KEY("key"),

    /**
     * 去重合并数量
     */
    DOC_COUNT("docCount"),

    ;

    private final String fieldName;
}
