package cn.yzw.jc2.common.transfer.enums;

import lombok.Getter;
/**
 * @Description: 读类型
 * @Author: lbl
 * @Date: 2024/10/18
 **/
@Getter
public enum ReadeTypeEnum {

    /**
     * ：READE_OLD_TABLE:读老表,
     */
    READE_OLD_TABLE,
    /**
     * READE_NEW_TABLE：读新表
     *  如果writeType=WRITE_NEW_TABLE，强制读新表
     */
    READE_NEW_TABLE
}
