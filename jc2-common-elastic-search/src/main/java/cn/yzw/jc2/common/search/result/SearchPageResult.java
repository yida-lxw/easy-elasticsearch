package cn.yzw.jc2.common.search.result;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import lombok.Data;

/**
 * @Description: 分页查询结果
 * @Author: lbl
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
    private int totalPage;
    /**
     * 数据集合
     */
    private List<T> records;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
