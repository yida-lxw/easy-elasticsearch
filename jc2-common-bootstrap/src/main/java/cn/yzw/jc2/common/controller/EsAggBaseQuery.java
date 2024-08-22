package cn.yzw.jc2.common.controller;

import java.io.Serializable;

import cn.yzw.jc2.common.search.annotation.EsEquals;
import cn.yzw.jc2.common.search.annotation.agg.EsAggNested;
import cn.yzw.jc2.common.search.annotation.agg.EsAggTerms;
import cn.yzw.jc2.common.search.annotation.agg.EsAggs;
import cn.yzw.jc2.common.search.annotation.agg.EsAvg;
import cn.yzw.jc2.common.search.annotation.agg.EsCardinality;
import cn.yzw.jc2.common.search.annotation.agg.EsCount;
import cn.yzw.jc2.common.search.annotation.agg.EsFilter;
import cn.yzw.jc2.common.search.annotation.agg.EsMax;
import cn.yzw.jc2.common.search.annotation.agg.EsSum;
import lombok.Data;

/**
 * es查询基类
 *
 * @author: zhangzhibao
 * @version: 1.0.0
 * @date: 2022-08-11 20:24
 */
@Data
public class EsAggBaseQuery implements Serializable {
    @EsEquals
    private Integer               deleted;
    @EsAggs
    private EsAggTestTermsRequest esAggTestTermsRequest = new EsAggTestTermsRequest();

    @Data
    public static class EsAggTestTermsRequest {

        @EsCount(aggName = "count_creatorId")
        private Long             creatorId;
        @EsAggTerms(aggName = "projectName_terms", name = "projectName", hasSubAgg = true)
        private EsAggTestRequest esAggTestRequest = new EsAggTestRequest();
        @EsAggNested(aggName = "nestedRequest", name = "planMonthlyProjectRelList")
        private EsAggTestNestedRequest nestedRequest    = new EsAggTestNestedRequest();
    }

    @Data
    public static class EsAggTestRequest {
        @EsMax(aggName = "max_creatorId")
        private Long             creatorId;
        @EsAvg(aggName = "avg_updaterId")
        @EsSum(aggName = "sum_updaterId")
        private Long             updaterId;
        @EsFilter
        private EsAggFilterQuery filterQuery = new EsAggFilterQuery();

        @Data
        public static class EsAggFilterQuery {
            @EsEquals
            private String creatorName = "呵呵1";
        }
    }

    @Data
    public static class EsAggTestNestedRequest {

        @EsAggTerms(aggName = "nestedProjectName", name = "projectName")
        private String projectName;
        @EsCardinality(aggName = "projectNameCardinality", name = "projectName")
        private String project;
    }
}
