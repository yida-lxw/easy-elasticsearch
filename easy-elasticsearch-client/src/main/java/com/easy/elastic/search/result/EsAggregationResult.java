package com.easy.elastic.search.result;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.easy.elastic.search.enums.EsAggTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * es 聚合查询结果
 * @author liangbaole
 */
@Getter
@Setter
@ToString
public class EsAggregationResult implements Serializable {

    /**
     * 聚合查询命名
     */
    private String                    name;

    /**
     * 聚合类型
     *
     * @see EsAggTypeEnum
     */
    private String                    aggregationType;

    /**
     * 查询结果，map key 与 aggregationType 绑定
     *
     */
    private List<Map<String, Object>> fieldAndValueList;

    /**
     * <p>
     * 剩余未被查询的数量 sumOtherCount = total - count
     */
    private Long                      sumOtherCount;
}
