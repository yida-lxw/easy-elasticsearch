package cn.yzw.jc2.model;

import lombok.Data;

import java.util.List;

/**
 * @author liangbaole
 * @version 1.0
 * @desc
 * @ClassName DMigrationJobRequest
 * @date 2022/9/16 10:56
 */
@Data
public class DMigrationJobQueryDO {
    /**
     * 1.0数据库名
     */
    private String     databaseName;
    /**
     * 1.0表名
     */
    private String     table;

    /**
     * 开始值，可空
     */
    private Long       minSysNo;

    /**
     * 结束值，可空
     */
    private Long       maxSysNo;

    /**
     * 指定sysNoList
     */
    private List<Long> sysNoList;

    /**
     * 每批条数
     */
    private Integer    limit = 1000;
    /**
     * sql条件
     */
    private String     sqlCondition;
}
