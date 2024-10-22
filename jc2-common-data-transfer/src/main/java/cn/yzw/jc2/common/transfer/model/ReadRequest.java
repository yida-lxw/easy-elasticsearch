package cn.yzw.jc2.common.transfer.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @description: 读请求
 * @author: yangle
 * @date: 2024/10/22
 **/
@Data
public class ReadRequest implements Serializable {
    /**
     * 表名
     */
    private String     table;

    /**
     * 开始值，可空
     */
    private Long       startId;

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
     * sql语句
     */
    private String     querySql;

    /**
     * 数据库类型
     */
    private String     datasourceType = "MySql";
}
