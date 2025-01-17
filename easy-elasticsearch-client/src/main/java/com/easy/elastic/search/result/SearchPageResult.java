package com.easy.elastic.search.result;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import lombok.Data;

/**
 * @Description: 分页查询结果
 * @Author: liangbaole
 * @Date: 2023/4/13
 **/
@Data
public class SearchPageResult<T> implements Serializable {


    /**
     * 当前页码
     */
    private int pageNum;
    /**
     * 每页数量
     */
    private int pageSize;
    /**
     * 记录总数
     */
    private long totalCount;
    /**
     * 页码总数
     */
    private long totalPage;
    /**
     * 数据集合
     */
    private List<T> records;
    /**
     * 聚合结果
     */
    private Map<String, EsAggregationResult> aggregationResult;

}
