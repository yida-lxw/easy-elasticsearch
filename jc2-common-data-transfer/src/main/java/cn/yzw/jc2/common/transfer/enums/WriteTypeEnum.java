package cn.yzw.jc2.common.transfer.enums;

import lombok.Getter;

/**
 * @Description: 写类型
 * @Author: lbl
 * @Date: 2024/10/18
 **/
@Getter
public enum WriteTypeEnum {
    /**
     * 写类型，ONLY_WRITE_OLD_TABLE:写老表,
     */
    ONLY_WRITE_OLD_TABLE,
    /**
     * ONLY_WRITE_NEW_TABLE：写新表
     */
    ONLY_WRITE_NEW_TABLE,
    /**
     * WRITE_ALL_TABLE：新老表都写
     */
    WRITE_ALL_TABLE
}
