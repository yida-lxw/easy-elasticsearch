package cn.yzw.jc2.common.transfer.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * @Description: 数据核对job
 * @Author: lbl 
 * @Date: 2024/10/24
 **/
@Data
public class DTransferVerifyJobRequest implements Serializable {

    /**
     * 老表名，必填
     */
    private String       olbTable;
    /**
     * 基于老表开始的最小id
     */
    private Long         olbTableStartId;
    /**
     * 基于老表结束的最小id
     */
    private Long         olbTableEndId;
    /**
     * 新表参数，必填
     */
    private NewTable     newTable;

    /**
     * 要核对的列，可空
     */
    private List<String> columns;

    /**
     * 核对忽略的列，默认忽略id,update_time,create_time
     */
    private List<String> ignoreColumns;
    /**
     * 表唯一键（新老表都唯一），必填
     */
    private String       primaryKeyName;
    /**
     * 分片键，必填
     */
    private String       shardingKeyName;

    /**
     * 核对类型，可空
     * COMPARE_NEW_TABLE_BY_OLD_TABLE： 基于老表对新表的字段一致性进行比较，不一致，更新新表数据
     *  COMPARE_OLD_TABLE_BY_NEW_TABLE 基于新表查询数据在老表是否存在，不存在则删除新表数据
     *  COMPARE_ALL 都执行
     */
    private String       verifyType;

    /**
     * 线程数
     */
    private Integer      threadNum;

    /**
     * 每批条数
     */
    private Integer      limit = 100;
    /**
     * 数据源名称，多数据源必须配置
     */
    private String       dataSourceName;

    @Data
    public static class NewTable implements Serializable {
        /**
         * 逻辑表名（分表的话，传逻辑表名），必填
         */
        private String  newTableLogicName;
        /**
         * 新表真实表名前缀示例：user_
         */
        private String  newTableRealNamePrefix;
        /**
         * 新表分片数
         */
        private Integer newTableShardNum;
        /**
         * 基于新表的开始分片
         */
        private Integer newTableStartShardNum;
    }

}
