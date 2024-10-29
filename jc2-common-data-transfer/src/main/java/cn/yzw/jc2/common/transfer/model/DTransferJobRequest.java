package cn.yzw.jc2.common.transfer.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author liangbaole
 * @version 1.0
 * @desc 数据迁移定时任务入参
 * @ClassName DMigrationJobRequest
 * @date 2022/9/16 10:56
 */
@Data
public class DTransferJobRequest implements Serializable {

    /**
     * 表名
     */
    private String     sourceTable;

    /**
     * 新表
     */
    private String     targetTable;

    /**
     * 开始值，可空
     */
    private Long       startId        = 0L;

    /**
     * 结束值，可空
     */
    private Long       endId;

    /**
     * 指定sysNo列表
     */
    private List<Long> idList;

    /**
     * 每批条数
     */
    private Integer    limit          = 10;

    /**
     * 线程数
     */
    private Integer    threadCount    = 1;

    /**
     * sql语句
     */
    private String     querySql;

    /**
     * 数据库类型
     */
    private String     datasourceType = "MySql";

    private String     jobId;

    private Boolean    ignoreId       = Boolean.TRUE;

    /**
     * 数据源名称，多数据源必须配置
     */
    private String     dataSourceName;
}
