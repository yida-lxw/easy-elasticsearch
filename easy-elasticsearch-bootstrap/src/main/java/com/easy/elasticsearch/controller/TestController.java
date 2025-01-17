package com.easy.elasticsearch.controller;

import com.easy.elastic.search.client.EsQueryClient;
import com.easy.elastic.search.enums.EsSearchTypeEnum;
import com.easy.elastic.search.request.DynamicSearchField;
import com.easy.elastic.search.request.EsBaseSearchParam;
import com.easy.elastic.search.request.SearchAfterRequest;
import com.easy.elastic.search.request.SearchPageRequest;
import com.easy.elastic.search.result.SearchAfterResult;
import com.easy.elastic.search.result.SearchPageResult;
import com.easy.elastic.search.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
public class TestController {

    @Autowired
    private EsQueryClient esQueryService;

    @GetMapping("/rest/test")
    public String test() {
        SearchAfterRequest<EsBaseSearchParam> request = new SearchAfterRequest<>();

        //DefaultEsBaseSearchParam query = new DefaultEsBaseSearchParam();
        EsOrgMultiQuery orgMultiQuery=new EsOrgMultiQuery();
        orgMultiQuery.setOrgCode("2000");
        orgMultiQuery.setOrgCodeContainSub("2000i000");
        List<EsOrgMultiQuery> orgMultiQueryList=new ArrayList<>();
        orgMultiQueryList.add(orgMultiQuery);

        DefaultEsBaseSearchParam.DefaultEsBaseSearchParamBuilder defaultEsBaseSearchParamBuilder = new DefaultEsBaseSearchParam.DefaultEsBaseSearchParamBuilder();
        DefaultEsBaseSearchParam defaultEsBaseSearchParam = defaultEsBaseSearchParamBuilder.setEsOrgMultiQuery(orgMultiQueryList).build();
        request.setParam(defaultEsBaseSearchParam);
        request.setPageSize(2);
        request.setIndex("alias_idx_search_info_qa");
        SearchAfterResult<Map> afterResult = esQueryService.searchAfter(request, Map.class);
        int i = 1;
        while (i++ < 3) {
            request.setSearchAfterList(afterResult.getSearchAfterList());
            afterResult = esQueryService.searchAfter(request, Map.class);
        }
        return JsonUtils.writeAsJson(afterResult);
    }

    @GetMapping("/rest/search")
    public String search() {
        SearchPageRequest<EsBaseSearchParam> request = new SearchPageRequest<>();
        EsOrgMultiQuery orgMultiQuery = new EsOrgMultiQuery();
        orgMultiQuery.setOrgCode("2000");
        orgMultiQuery.setOrgCodeContainSub("2000i000");
        List<EsOrgMultiQuery> orgMultiQueryList=new ArrayList<>();
        orgMultiQueryList.add(orgMultiQuery);

        Map<String, DynamicSearchField> dynamicFieldsMap = new HashMap<>();
        DefaultEsBaseSearchParam.DefaultEsBaseSearchParamBuilder defaultEsBaseSearchParamBuilder = new DefaultEsBaseSearchParam.DefaultEsBaseSearchParamBuilder();
        //DefaultEsBaseSearchParam defaultEsBaseSearchParam = defaultEsBaseSearchParamBuilder.setEsOrgMultiQuery(orgMultiQuery).build();
        defaultEsBaseSearchParamBuilder.setEsOrgMultiQuery(orgMultiQueryList).setDynamicFieldsMap(dynamicFieldsMap);
        DefaultEsBaseSearchParam defaultEsBaseSearchParam = defaultEsBaseSearchParamBuilder.build();
        DynamicSearchField field = new DynamicSearchField();
        field.setSearchType(EsSearchTypeEnum.esLike.name());
        field.setValue("门窗");
        dynamicFieldsMap.put("catName", field);
        field=new DynamicSearchField();
        field.setSearchType(EsSearchTypeEnum.esEquals.name());
        field.setValue("234353545");
        dynamicFieldsMap.put("code", field);
        request.setParam(defaultEsBaseSearchParam);
        request.setPageSize(2);
        request.setIndex("alias_idx_search_info_qa");
        SearchPageResult<Map> afterResult = esQueryService.search(request, Map.class);
        return  JsonUtils.writeAsJson(afterResult);
    }

    @GetMapping("/rest/agg")
    public String agg() {
        SearchPageRequest<EsBaseSearchParam> request = new SearchPageRequest<>();
        EsAggBaseQuery query = new EsAggBaseQuery();
        request.setParam(query);
        request.setIndex("alias_idx_search_info_qa");
        return  JsonUtils.writeAsJson(esQueryService.agg(request));
    }
}
