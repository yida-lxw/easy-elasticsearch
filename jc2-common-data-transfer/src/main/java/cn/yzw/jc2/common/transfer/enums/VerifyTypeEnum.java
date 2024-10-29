package cn.yzw.jc2.common.transfer.enums;

import lombok.Getter;

/**
 * @Description: 对账类型
 * @Author: lbl
 * @Date: 2024/10/18
 **/
@Getter
public enum VerifyTypeEnum {

    /**
     * 基于老表对新表的字段一致性进行比较，不一致，更新新表数据
     */
    COMPARE_NEW_TABLE_BY_OLD_TABLE,
    /**
     * 基于新表查询数据在老表是否存在，不存在则删除新表数据
     */
    COMPARE_OLD_TABLE_BY_NEW_TABLE,
    /**
     * 都执行
     */
    COMPARE_ALL
}
