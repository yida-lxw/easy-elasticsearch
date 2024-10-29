package cn.yzw.jc2.common.transfer.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @description:
 * @author: yangle
 * @date: 2024/10/22
 **/
@Data
public class WriteRequest implements Serializable {

    /**
     * 写入模板
     */
    private String         writeTemplate;

    /**
     * 任务id
     */
    private String         jobId;

    /**
     * 参数
     */
    private List<Object[]> params;

    /**
     * 新表名
     */
    private String         targetTable;

    /**
     * 数据源名称，多数据源必须配置
     */
    private String         dataSourceName;
}
