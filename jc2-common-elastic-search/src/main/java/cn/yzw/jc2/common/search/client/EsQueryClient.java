package cn.yzw.jc2.common.search.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import cn.yzw.infra.component.utils.AssertUtils;
import cn.yzw.jc2.common.search.parse.EsQueryParse;
import cn.yzw.jc2.common.search.parse.EsQueryResultParse;
import cn.yzw.jc2.common.search.request.EsSearchBase;
import cn.yzw.jc2.common.search.request.ScrollRequest;
import cn.yzw.jc2.common.search.request.SearchAfterRequest;
import cn.yzw.jc2.common.search.request.SearchPageRequest;
import cn.yzw.jc2.common.search.result.EsAggregationResult;
import cn.yzw.jc2.common.search.result.ScrollResult;
import cn.yzw.jc2.common.search.result.SearchAfterResult;
import cn.yzw.jc2.common.search.result.SearchPageResult;
import lombok.extern.slf4j.Slf4j;

/**
 * es查询客户端
 *
 * @author: lbl
 * @version: 1.0.0
 * @date: 2022-08-12 08:57
 */
@Slf4j
public class EsQueryClient {

    @Value("${es.query.sleep.ms:500}")
    private Long                esQuerySleepMs;

    @Value("${es.query.max-size:10000}")
    private Integer             esQueryMaxSize;
    @Value("${es.query.like.max-size:50}")
    public Integer              esQueryLikeMaxSize;

    @Value("#{${es.query.sleep.index.config:{}}}")
    private Map<String, Long>   esIndexSleepConfigMap = new HashMap<>();

    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * @Description: 分页查询es,默认支持10000条，支持排序字段显示传递，推荐使用此方法
     * @Author: lbl 
     * @Date:  2023/4/6 20:24
     * @param: oclass 出参类型
     * @return:
     **/
    public <E, O> SearchPageResult<O> search(SearchPageRequest<E> request, Class<O> oclass) {

        SearchPageResult<O> pageResult = new SearchPageResult<>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            sleep(request);
            AssertUtils.isTrue(request.getPageNum() * request.getPageSize() <= esQueryMaxSize,
                "仅支持查询前" + esQueryMaxSize + "条, 请增加查询条件缩小范围");

            SearchRequest searchRequest = EsQueryParse.convert2EsPageQuery(request);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            pageResult.setTotalCount(hits.getTotalHits().value);
            List<O> list = EsQueryResultParse.getRs(oclass, searchResponse);
            pageResult.setRecords(list);
            pageResult.setPageNum(request.getPageNum());
            pageResult.setPageSize(request.getPageSize());
            pageResult.setTotalPage(
                    Long.valueOf((pageResult.getTotalCount() - 1) / Long.valueOf(request.getPageSize()) + 1L).intValue());
            pageResult.setAggregationResult(EsQueryResultParse.injectAggregations(searchResponse.getAggregations()));
        } catch (IOException ex) {
            log.error("es查询时出现异常, index: {}, params: {}", request.getIndex(), request, ex);
        } finally {
            stopWatch.stop();
            log.info("EsQueryClient.search耗时{}", stopWatch.getTotalTimeMillis());
        }
        return pageResult;
    }

    private <E> void sleep(SearchPageRequest<E> request) {
        if (Boolean.TRUE.equals(request.getSleep())) {
            try {
                Thread.sleep(esQuerySleepMs);
            } catch (InterruptedException e) {
                log.warn("sleep failed");
            }
        } else {
            if (esIndexSleepConfigMap.containsKey(request.getIndex())) {
                try {
                    Long time = esIndexSleepConfigMap.get(request.getIndex());
                    Thread.sleep(time == null ? esQuerySleepMs : time);
                } catch (InterruptedException e) {
                    log.warn("sleep failed");
                }
            }
        }
    }

    /**
     * searchAfter查询
     * @return 结果
     *  @Author: lbl 
     *  @Date:  2023/4/6 20:24
     *  @param:oclass 出参类型
     */
    public <E, O> SearchAfterResult<O> searchAfter(SearchAfterRequest<E> request,
                                                                        Class<O> oclass) {

        SearchAfterResult<O> pageResult = new SearchAfterResult<>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SearchRequest searchRequest = EsQueryParse.convertSearchAfter2Query(request);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            List<O> list = EsQueryResultParse.getRs(oclass, searchResponse);
            pageResult.setData(list);
            pageResult.setTotalCount(searchResponse.getHits().getTotalHits().value);
            // 获取最后一条记录, 并把 sort 的值赋值给 searchAfter
            SearchHit[] searchHits = searchResponse.getHits().getHits();
            if (searchHits != null && searchHits.length > 0) {
                Object[] sortValues = searchHits[searchHits.length - 1].getSortValues();
                if (sortValues != null) {
                    pageResult.setSearchAfterList(Arrays.stream(sortValues).collect(Collectors.toList()));
                }
            }
        } catch (IOException ex) {
            log.error("es查询时出现异常, index: {}, params: {}", request.getIndex(), request, ex);
        } finally {
            stopWatch.stop();
            log.info("EsQueryClient.searchAfter耗时{}", stopWatch.getTotalTimeMillis());
        }
        return pageResult;
    }

    /**
     * scroll查询
     * @return 结果
     * @param: oclass 出参类型
     */
    public <E, O> ScrollResult<O> scroll(ScrollRequest<E> request, Class<O> oclass) {
        ScrollResult<O> pageResult = new ScrollResult<>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            SearchResponse searchResponse;
            //第一次处理
            if (StringUtils.isBlank(request.getScrollId())) {
                SearchRequest searchRequest = EsQueryParse.convertScroll2Query(request);
                searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
                //根据游标滚动
            } else {
                SearchScrollRequest searchScrollRequest = new SearchScrollRequest(request.getScrollId());
                searchResponse = restHighLevelClient.scroll(searchScrollRequest, RequestOptions.DEFAULT);
            }

            List<O> list = EsQueryResultParse.getRs(oclass, searchResponse);
            pageResult.setScrollId(searchResponse.getScrollId());
            pageResult.setData(list);
            pageResult.setTotalCount(searchResponse.getHits().getTotalHits().value);
            //使用完后清除scroll
            if (CollectionUtils.isEmpty(pageResult.getData())) {
                ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.setScrollIds(Collections.singletonList(searchResponse.getScrollId()));
                restHighLevelClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            }
        } catch (IOException ex) {
            log.error("es查询时出现异常, index: {}, params: {}", request.getIndex(), request, ex);
        } finally {
            stopWatch.stop();
            log.info("EsQueryClient.scroll耗时{}", stopWatch.getTotalTimeMillis());
        }
        return pageResult;
    }

    /**
     * @Description: 聚合
     * @Author: lbl
     * @Date:  2023/4/6 20:24
     * @param: oclass 出参类型
     * @return:
     **/
    public <E> Map<String, EsAggregationResult> agg(SearchPageRequest<E> request) {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            request.setPageSize(0);
            request.setPageNum(0);
            SearchRequest searchRequest = EsQueryParse.convert2EsPageQuery(request);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            Map<String, EsAggregationResult> aggregationResultMap = EsQueryResultParse
                .injectAggregations(searchResponse.getAggregations());
            return aggregationResultMap;
        } catch (IOException ex) {
            log.error("es查询时出现异常, index: {}, params: {}", request.getIndex(), request, ex);
        } finally {
            stopWatch.stop();
            log.info("EsQueryClient.agg耗时{}", stopWatch.getTotalTimeMillis());
        }
        return null;
    }

}
