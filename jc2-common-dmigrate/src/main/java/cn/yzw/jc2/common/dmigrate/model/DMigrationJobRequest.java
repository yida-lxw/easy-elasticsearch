package cn.yzw.jc2.common.dmigrate.model;

import lombok.Data;

import java.util.List;

/**
 * @author liangbaole
 * @version 1.0
 * @desc 数据迁移定时任务入参
 * @ClassName DMigrationJobRequest
 * @date 2022/9/16 10:56
 */
@Data
public class DMigrationJobRequest {
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
     * 指定sysNo列表
     */
    private List<Long> sysNoList;

    /**
     * 每批条数
     */
    private Integer    limit           = 10;

    /**
     * 是否需要查询实体明细，true需要
     */
    private Boolean    isNeedQueryBody = false;

    /**
     * 线程数
     */
    private Integer    threadCount     = 1;
    /**
     * sql条件
     */
    private String     sqlCondition;
}
