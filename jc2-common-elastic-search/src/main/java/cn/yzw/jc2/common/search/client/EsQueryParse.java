package cn.yzw.jc2.common.search.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.join.query.HasChildQueryBuilder;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import cn.yzw.infra.component.utils.JsonUtils;
import cn.yzw.infra.component.utils.SpringContextUtils;
import cn.yzw.jc2.common.search.annotation.EsEquals;
import cn.yzw.jc2.common.search.annotation.EsHasChildRelation;
import cn.yzw.jc2.common.search.annotation.EsHasParentRelation;
import cn.yzw.jc2.common.search.annotation.EsIn;
import cn.yzw.jc2.common.search.annotation.EsIsNull;
import cn.yzw.jc2.common.search.annotation.EsLike;
import cn.yzw.jc2.common.search.annotation.EsMatch;
import cn.yzw.jc2.common.search.annotation.EsMatchPhrase;
import cn.yzw.jc2.common.search.annotation.EsMulti;
import cn.yzw.jc2.common.search.annotation.EsNested;
import cn.yzw.jc2.common.search.annotation.EsNotEquals;
import cn.yzw.jc2.common.search.annotation.EsNotLike;
import cn.yzw.jc2.common.search.annotation.EsNotNull;
import cn.yzw.jc2.common.search.annotation.EsNotNullFields;
import cn.yzw.jc2.common.search.annotation.EsRange;
import cn.yzw.jc2.common.search.request.Order;
import cn.yzw.jc2.common.search.request.ScrollRequest;
import cn.yzw.jc2.common.search.request.SearchAfterRequest;
import cn.yzw.jc2.common.search.request.SearchBaseRequest;
import cn.yzw.jc2.common.search.request.SearchPageRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * ES查询解析
 *
 * @author: zhangzhibao
 * @version: 1.0.0
 * @date: 2022-08-11 20:16
 */
@Slf4j
public class EsQueryParse {
    private EsQueryParse() {
    }

    public static SearchRequest convert2EsPageQuery(SearchPageRequest request) {

        //构建查询
        SearchSourceBuilder sourceBuilder = buildBoolQueryBuilder(request);
        // 设置分页
        sourceBuilder.from((request.getPageNum() - 1) * request.getPageSize());
        sourceBuilder.size(request.getPageSize());

        // 设置返回真实总数
        if (request.getPageSize() > 0) {
            sourceBuilder.trackTotalHits(true);
        }
        log.info("es query string: GET {}/_search \n {}", request.getIndex(),
                JsonUtils.writeAsJson(JsonUtils.readAsMap(sourceBuilder.toString())));

        SearchRequest searchRequest = new SearchRequest(request.getIndex());
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

    public static <E> SearchRequest convertSearchAfter2Query(SearchAfterRequest<E> searchAfterRequest) {

        //构建查询
        SearchSourceBuilder sourceBuilder = buildBoolQueryBuilder(searchAfterRequest);
        // 默认排序字段
        sourceBuilder.size(searchAfterRequest.getPageSize());
        sourceBuilder.from(0);
        // 设置分页或者searchAfter
        if (CollectionUtils.isNotEmpty(searchAfterRequest.getSearchAfterList())) {
            sourceBuilder.searchAfter(searchAfterRequest.getSearchAfterList().toArray());
        }

        log.info("es query string: GET {}/_search \n {}", searchAfterRequest.getIndex(),
                JsonUtils.writeAsJson(JsonUtils.readAsMap(sourceBuilder.toString())));

        SearchRequest searchRequest = new SearchRequest(searchAfterRequest.getIndex());
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

    public static SearchRequest convert2AggQuery(String index, Object param, Supplier<QueryBuilder>... customQueries) {
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = getBoolQueryBuilder(param);

        if (customQueries != null) {
            for (Supplier<QueryBuilder> customQuery : customQueries) {
                if (customQuery != null) {
                    boolQueryBuilder.filter(customQuery.get());
                }
            }
        }
        // 设置分页
        sourceBuilder.from(0);
        sourceBuilder.size(0);
        // 查询逻辑
        sourceBuilder.query(boolQueryBuilder);
        log.info("es query string agg: GET {}/_search \n {}", index,
                JsonUtils.writeAsJson(JsonUtils.readAsMap(sourceBuilder.toString())));
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }
    public static <E> SearchRequest convertScroll2Query(ScrollRequest<E> scrollRequest) {

        //构建查询
        SearchSourceBuilder sourceBuilder = buildBoolQueryBuilder(scrollRequest);

        sourceBuilder.size(scrollRequest.getPageSize());

        SearchRequest searchRequest = new SearchRequest(scrollRequest.getIndex());
        searchRequest.source(sourceBuilder);
        Scroll scroll = new Scroll(TimeValue.timeValueMinutes(scrollRequest.getKeepAliveTimeMinute()));
        searchRequest.scroll(scroll);
        log.info("es query string: GET {}/_search \n {}", scrollRequest.getIndex(),
                JsonUtils.writeAsJson(JsonUtils.readAsMap(sourceBuilder.toString())));

        return searchRequest;
    }

    protected static <T> SearchSourceBuilder buildBoolQueryBuilder(SearchBaseRequest<T> requestParam) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = buildBoolQueryBuilder(requestParam.getParam(), sourceBuilder,
                requestParam.getCustomQueries());
        //增加租户处理
        if (requestParam.getDealTenant()
                && StringUtils.isNotBlank(requestParam.getTenantId())) {
            TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("tenantId", requestParam.getTenantId());
            boolQueryBuilder.filter(termQueryBuilder);
        }
        sourceBuilder.query(boolQueryBuilder);
        //返回字段指定
        List<String> sourceIncludeFields = requestParam.getSourceIncludeFields();
        List<String> sourceExcludeFields = requestParam.getSourceExcludeFields();
        sourceBuilder.fetchSource(
                CollectionUtils.isEmpty(sourceIncludeFields) ? null
                        : sourceIncludeFields.toArray(new String[sourceIncludeFields.size()]),
                CollectionUtils.isEmpty(sourceExcludeFields) ? null
                        : sourceExcludeFields.toArray(new String[sourceExcludeFields.size()]));
        // 默认排序字段
        setSortFields(requestParam.getOrderByFieldList(), sourceBuilder);
        return sourceBuilder;
    }

    private static <T> BoolQueryBuilder buildBoolQueryBuilder(T request, SearchSourceBuilder sourceBuilder,
                                                              Supplier<QueryBuilder>[] customQueries) {

        BoolQueryBuilder boolQueryBuilder;
        if (request == null) {
            boolQueryBuilder = new BoolQueryBuilder();
        } else {
            boolQueryBuilder = getBoolQueryBuilder(request);
        }
        if (customQueries != null && customQueries.length > 0) {
            for (int i = 0; i < customQueries.length; i++) {
                if (customQueries[i] != null) {
                    boolQueryBuilder.filter(customQueries[i].get());
                }
            }
        }

        return boolQueryBuilder;
    }

    /**
     * 设置排序字段
     * @param orderList
     * @param sourceBuilder
     */
    private static void setSortFields(List<Order> orderList, SearchSourceBuilder sourceBuilder) {
        if (CollectionUtils.isEmpty(orderList)) {
            sourceBuilder.sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));
            sourceBuilder.sort(SortBuilders.fieldSort("id").order(SortOrder.DESC));
        } else {
            for (Order order : orderList) {
                sourceBuilder.sort(order.getOrderByField(), SortOrder.fromString(order.getOrderType()));
            }
        }
    }

    private static <T> BoolQueryBuilder getBoolQueryBuilder(Object object) {
        return getBoolQueryBuilder(object, null);
    }

    private static BoolQueryBuilder getBoolQueryBuilder(Object object, String nestedPath) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        Class<?> clazz = object.getClass();
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            //当父类为null的时候说明到达了最上层的父类(Object类).
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            //得到父类,然后赋给自己
            clazz = clazz.getSuperclass();
        }
        nestedPath = nestedPath == null ? "" : nestedPath;
        try {
            for (Field field : fields) {
                if ("serialVersionUID".equals(field.getName())) {
                    continue;
                }
                Object value = ClassUtils.getPublicMethod(object.getClass(), "get" + captureName(field.getName()))
                        .invoke(object);

                if (value == null || field.getType() == String.class && StringUtils.isBlank((String) value)) {
                    continue;
                }

                try {
                    // EsMulti优先
                    if (field.isAnnotationPresent(EsMulti.class)) {
                        // 多条件组合：品类权限、组织权限场景
                        BoolQueryBuilder bool = QueryBuilders.boolQuery();
                        if (field.getType().isAssignableFrom(List.class)) {
                            List<Object> vl = (List) value;
                            String finalNestedPath = nestedPath;
                            vl.forEach(e -> {
                                // 数据权限，需要是or / should查询
                                bool.should(getBoolQueryBuilder(e, finalNestedPath));
                            });
                        } else {
                            bool.filter(getMultiQuery(field, value, nestedPath));
                        }
                        boolQueryBuilder.filter(bool);
                    } else {
                        if (field.isAnnotationPresent(EsLike.class)) {
                            String val = wildcardOptimize((String) value);
                            WildcardQueryBuilder query = getLikeQuery(field, val, nestedPath);
                            boolQueryBuilder.filter(query);
                        }
                        if (field.isAnnotationPresent(EsNotLike.class)) {
                            String val = wildcardOptimize((String) value);
                            BoolQueryBuilder query = getNotLikeQuery(field, val, nestedPath);
                            boolQueryBuilder.filter(query);
                        }
                        if (field.isAnnotationPresent(EsEquals.class)) {
                            TermQueryBuilder query = getEqualsQuery(field, value, nestedPath);

                            boolQueryBuilder.filter(query);
                        }
                        if (field.isAnnotationPresent(EsNotEquals.class)) {
                            BoolQueryBuilder query = getNotEqualsQuery(field, value, nestedPath);
                            boolQueryBuilder.filter(query);

                        }
                        if (field.isAnnotationPresent(EsRange.class)) {
                            RangeQueryBuilder query = getRangeQuery(field, value, nestedPath);
                            boolQueryBuilder.filter(query);
                        }
                        if (field.isAnnotationPresent(EsIn.class)) {
                            EsIn esIn = field.getAnnotation(EsIn.class);
                            if (esIn.allowEmpty() || CollectionUtils.isNotEmpty((List<?>) value)) {
                                QueryBuilder query = getInQuery(field, (List<?>) value, nestedPath);
                                boolQueryBuilder.filter(query);
                            }
                        }
                        if (field.isAnnotationPresent(EsNotNull.class)) {
                            ExistsQueryBuilder query = getNotNullQuery(field, nestedPath);
                            boolQueryBuilder.filter(query);
                        }
                        if (field.isAnnotationPresent(EsIsNull.class)) {
                            BoolQueryBuilder query = getIsNullQuery(field, nestedPath);
                            boolQueryBuilder.filter(query);
                        }
                        if (field.isAnnotationPresent(EsMatchPhrase.class)) {
                            MatchPhraseQueryBuilder matchPhrase = getMatchPhrase(field, value, nestedPath);
                            boolQueryBuilder.filter(matchPhrase);
                        }
                        if (field.isAnnotationPresent(EsMatch.class)) {
                            MatchQueryBuilder match = getMatch(field, value, nestedPath);
                            boolQueryBuilder.filter(match);
                        }
                        if (field.isAnnotationPresent(EsNotNullFields.class)) {
                            List<ExistsQueryBuilder> query = getNotNullQuery((List<String>) value, nestedPath);
                            Optional.ofNullable(query).orElse(new ArrayList<>()).forEach(boolQueryBuilder::filter);
                        }
                        if (field.isAnnotationPresent(EsNested.class)) {
                            NestedQueryBuilder query = getNestedQuery(field, value);
                            boolQueryBuilder.filter(query);
                        }
                        if (field.isAnnotationPresent(EsHasChildRelation.class)) {
                            field.setAccessible(true);
                            QueryBuilder builder = getBoolQueryBuilder(field.get(object), nestedPath);
                            HasChildQueryBuilder childQueryBuilder = getHasChildQueryBuilder(
                                    field.getAnnotation(EsHasChildRelation.class), builder);
                            boolQueryBuilder.filter(childQueryBuilder);
                        }
                        if (field.isAnnotationPresent(EsHasParentRelation.class)) {
                            field.setAccessible(true);
                            QueryBuilder builder = getBoolQueryBuilder(field.get(object), nestedPath);
                            EsHasParentRelation relation = field.getAnnotation(EsHasParentRelation.class);
                            HasParentQueryBuilder childQueryBuilder = getHasParentQueryBuilder(builder, relation);
                            boolQueryBuilder.filter(childQueryBuilder);
                        }
                    }
                } catch (Exception e) {
                    log.warn("ES查询解析异常1: ", e);
                }

            }
        } catch (Exception e) {
            log.warn("ES查询解析异常", e);
        }

        //父子文档直接打在类上
        BoolQueryBuilder boolQueryBuilderTop = null;
        if (object != null && object.getClass().isAnnotationPresent(EsHasChildRelation.class)) {
            // 外面再包裹一层query
            boolQueryBuilderTop = QueryBuilders.boolQuery();
            HasChildQueryBuilder hashChildQuery = getHasChildQueryBuilder(
                    object.getClass().getAnnotation(EsHasChildRelation.class), boolQueryBuilder);
            boolQueryBuilderTop.filter(hashChildQuery);
        } else if (object != null && object.getClass().isAnnotationPresent(EsHasParentRelation.class)) {
            // 外面再包裹一层query
            boolQueryBuilderTop = QueryBuilders.boolQuery();
            EsHasParentRelation relation = object.getClass().getAnnotation(EsHasParentRelation.class);
            HasParentQueryBuilder childQueryBuilder = getHasParentQueryBuilder(boolQueryBuilder, relation);

            boolQueryBuilderTop.filter(childQueryBuilder);
        } else {
            boolQueryBuilderTop = boolQueryBuilder;
        }
        return boolQueryBuilderTop;
    }

    private static String wildcardOptimize(String value) {
        String val = value;
        EsQueryClient queryClient = SpringContextUtils.getBean(EsQueryClient.class);
        if (val.length() > queryClient.esQueryLikeMaxSize) {
            log.info("检索字段太长了，我截取了哈^_^");
            val = value.substring(0, queryClient.esQueryLikeMaxSize);
        }
        val = replaceSpecialChar(val);
        return val;
    }

    private static String replaceSpecialChar(String val) {
        if (val.contains("*")) {
            val = val.replace("*", "\\*");
        }
        if (val.contains("?")) {
            val = val.replace("?", "\\?");
        }
        return val;
    }

    private static HasParentQueryBuilder getHasParentQueryBuilder(QueryBuilder builder, EsHasParentRelation relation) {
        HasParentQueryBuilder childQueryBuilder = new HasParentQueryBuilder(relation.parentType(), builder, false);
        if (relation.returnInnerHits()) {
            InnerHitBuilder innerHitBuilder = StringUtils.isNotBlank(relation.innerHitsName())
                    ? new InnerHitBuilder(relation.innerHitsName())
                    : new InnerHitBuilder();
            innerHitBuilder.setSize(relation.innerHitsSize());
            childQueryBuilder.innerHit(innerHitBuilder);
        }

        return childQueryBuilder;
    }

    private static HasChildQueryBuilder getHasChildQueryBuilder(EsHasChildRelation relation, QueryBuilder builder) {
        HasChildQueryBuilder childQueryBuilder = new HasChildQueryBuilder(relation.type(), builder, ScoreMode.None);
        if (relation.returnInnerHits()) {
            InnerHitBuilder innerHitBuilder = StringUtils.isNotBlank(relation.innerHitsName())
                    ? new InnerHitBuilder(relation.innerHitsName())
                    : new InnerHitBuilder();
            innerHitBuilder.setSize(relation.innerHitsSize());
            childQueryBuilder.innerHit(innerHitBuilder);
        }
        return childQueryBuilder;
    }

    private static QueryBuilder getInQuery(Field field, List<?> value, String nestedPath) {
        EsIn esIn = field.getAnnotation(EsIn.class);

        String filedName = getFiledName(field, esIn.name(), nestedPath);
        List<?> _value = CollectionUtils.isEmpty(value) ? new ArrayList<>()
                : value.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (!esIn.leftLike() && !esIn.rightLike()) {
            //单纯的in
            return QueryBuilders.termsQuery(filedName, _value);
        } else {
            //对in的每项进行like处理
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            for (Object item : _value) {
                String likeValue = (String) item;
                if (esIn.leftLike()) {
                    likeValue = "*" + likeValue;
                }
                if (esIn.rightLike()) {
                    likeValue = likeValue + "*";
                }
                boolQueryBuilder.should(QueryBuilders.wildcardQuery(filedName, likeValue));
            }
            return boolQueryBuilder;
        }
    }

    private static RangeQueryBuilder getRangeQuery(Field field, Object value, String nestedPath) {
        EsRange esRange = field.getAnnotation(EsRange.class);
        String filedName = getFiledName(field, esRange.name(), nestedPath);
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(filedName);
        if (esRange.lt()) {
            rangeQueryBuilder.lt(value).includeLower(esRange.includeLower()).includeUpper(esRange.includeUpper());
        }
        if (esRange.gt()) {
            rangeQueryBuilder.gt(value).includeLower(esRange.includeLower()).includeUpper(esRange.includeUpper());
        }
        return rangeQueryBuilder;
    }

    private static TermQueryBuilder getEqualsQuery(Field field, Object value, String nestedPath) {
        EsEquals esEquals = field.getAnnotation(EsEquals.class);
        String filedName = getFiledName(field, esEquals.name(), nestedPath);

        return QueryBuilders.termQuery(filedName, value);
    }

    private static BoolQueryBuilder getNotEqualsQuery(Field field, Object value, String nestedPath) {
        EsNotEquals esNotEquals = field.getAnnotation(EsNotEquals.class);
        String filedName = getFiledName(field, esNotEquals.name(), nestedPath);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        return boolQueryBuilder.mustNot(QueryBuilders.termQuery(filedName, value));
    }
    private static BoolQueryBuilder getIsNullQuery(Field field, String nestedPath) {
        EsIsNull esNotNull = field.getAnnotation(EsIsNull.class);
        String filedName = getFiledName(field, esNotNull.name(), nestedPath);
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        return boolQuery.mustNot(QueryBuilders.existsQuery(filedName));
    }
    private static MatchQueryBuilder getMatch(Field field, Object value, String nestedPath) {
        EsMatch esMatchPhrase = field.getAnnotation(EsMatch.class);
        String filedName = getFiledName(field, esMatchPhrase.name(), nestedPath);

        return QueryBuilders.matchQuery(filedName, value);
    }

    private static MatchPhraseQueryBuilder getMatchPhrase(Field field, Object value, String nestedPath) {
        EsMatchPhrase esMatchPhrase = field.getAnnotation(EsMatchPhrase.class);
        String filedName = getFiledName(field, esMatchPhrase.name(), nestedPath);

        return QueryBuilders.matchPhraseQuery(filedName, value);
    }
    private static WildcardQueryBuilder getLikeQuery(Field field, Object value, String nestedPath) {
        String likeValue = (String) value;
        EsLike esLike = field.getAnnotation(EsLike.class);
        String filedName = getFiledName(field, esLike.name(), nestedPath);
        if (esLike.leftLike()) {
            likeValue = "*" + likeValue;
        }
        if (esLike.rightLike()) {
            likeValue = likeValue + "*";
        }
        return QueryBuilders.wildcardQuery(filedName, likeValue);
    }

    private static BoolQueryBuilder getNotLikeQuery(Field field, Object value, String nestedPath) {
        String likeValue = (String) value;
        EsNotLike esNotLike = field.getAnnotation(EsNotLike.class);
        String filedName = getFiledName(field, esNotLike.name(), nestedPath);
        if (esNotLike.leftLike()) {
            likeValue = "*" + likeValue;
        }
        if (esNotLike.rightLike()) {
            likeValue = likeValue + "*";
        }
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        return boolQueryBuilder.mustNot(QueryBuilders.wildcardQuery(filedName, likeValue));
    }

    private static ExistsQueryBuilder getNotNullQuery(Field field, String nestedPath) {
        EsNotNull esNotNull = field.getAnnotation(EsNotNull.class);
        String filedName = getFiledName(field, esNotNull.name(), nestedPath);
        return QueryBuilders.existsQuery(filedName);
    }

    private static List<ExistsQueryBuilder> getNotNullQuery(List<String> value, String nestedPath) {
        if (CollectionUtils.isEmpty(value)) {
            return new ArrayList<>();
        }
        return value.stream().map(item -> getFiledName(item, nestedPath)).map(QueryBuilders::existsQuery)
                .collect(Collectors.toList());
    }

    private static NestedQueryBuilder getNestedQuery(Field field, Object object) {
        EsNested esNested = field.getAnnotation(EsNested.class);
        String nestedPath = getFiledName(field, esNested.name(), "");
        QueryBuilder boolQueryBuilder = getBoolQueryBuilder(object, nestedPath);
        NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery(nestedPath, boolQueryBuilder, ScoreMode.None);

        if (esNested.needInnerHits()) {
            InnerHitBuilder innerHitBuilder = new InnerHitBuilder();
            innerHitBuilder.setSize(esNested.innerHitsSize());
            nestedQueryBuilder.innerHit(innerHitBuilder);
        }
        return nestedQueryBuilder;
    }

    /**
     * @param field
     * @param value
     * @param nestedPath
     * @return
     */
    private static BoolQueryBuilder getMultiQuery(Field field, Object value, String nestedPath) {
        EsMulti esMulti = field.getAnnotation(EsMulti.class);
        BoolQueryBuilder boolQueryBuilder = getBoolQueryBuilder(value, nestedPath);
        return boolQueryBuilder;
    }

    private static String getFiledName(Field field, String name, String nestedPath) {
        String fileName = name;
        if (field != null) {
            fileName = StringUtils.isBlank(name) ? field.getName() : name;
        }
        if (StringUtils.isBlank(nestedPath)) {
            return fileName;
        }
        return nestedPath + "." + fileName;
    }

    private static String getFiledName(String name, String nestedPath) {
        return getFiledName(null, name, nestedPath);
    }

    public static String captureName(String name) {
        char[] cs = name.toCharArray();
        if ('a' <= cs[0] && cs[0] <= 'z') {
            cs[0] -= 32;
        }
        return String.valueOf(cs);
    }
}