package cn.yzw.jc2.common.dmigrate.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * @author lbl
 * @date 2022/9/14 13:52
 */
@Getter
@RequiredArgsConstructor
public enum OperateTypeEnum {
    // ===

    CREATE("增", "c"), UPDATE("改", "u"), DELETE("删", "d"),

    ;

    /** 描述信息 */
    private final String message;

    /** kafka操作类型 */
    private final String outsideType;

    /**
     * 通过外部类型获取枚举
     * 
     * @param outsideType 外部类型
     * @return 当前枚举
     */
    public static OperateTypeEnum getByOutsideType(String outsideType) {
        for (OperateTypeEnum value : values()) {
            if (Objects.equals(value.getOutsideType(), outsideType)) {
                return value;
            }
        }
        return null;
    }
}
