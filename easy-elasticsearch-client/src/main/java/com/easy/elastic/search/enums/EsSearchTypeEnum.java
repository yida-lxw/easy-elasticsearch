package com.easy.elastic.search.enums;

public enum EsSearchTypeEnum {
    /**
     * 等于
     */
    esEquals,
    /**
     * 不等于
     */
    esNotEquals,
    /**
     * 多值精确匹配
     */
    esIn,
    /**
     * 多值，not in
     */
    esNotIn,
    /**
     * 模糊搜索
     */
    esLike,
    /**
     * 模糊搜索不包含
     */
    esNotLike,
    /**
     * 范围匹配
     */
    esRange,
    /**
     * 可空
     */
    esIsNull,
    /**
     * 不可空
     */
    esIsNotNull,
    ;
}
