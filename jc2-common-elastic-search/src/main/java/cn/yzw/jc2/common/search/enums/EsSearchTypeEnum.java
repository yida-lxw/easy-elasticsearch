package cn.yzw.jc2.common.search.enums;

public enum EsSearchTypeEnum {
    /**
     * 等于
     */
    esEquals,
    /**
     * 多值精确匹配
     */
    esIn,
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
