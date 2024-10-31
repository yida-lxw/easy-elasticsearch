package com.easy.elasticsearch.controller;

import java.util.HashMap;
import java.util.Map;

import com.easy.elastic.search.client.EsQueryClient;
import com.easy.elastic.search.enums.EsSearchTypeEnum;
import com.easy.elastic.search.request.DynamicSearchField;
import com.easy.elastic.search.request.SearchAfterRequest;
import com.easy.elastic.search.request.SearchPageRequest;
import com.easy.elastic.search.result.SearchAfterResult;
import com.easy.elastic.search.result.SearchPageResult;
import com.easy.elastic.search.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class TestController {

    @Autowired
    private EsQueryClient esQueryService;

    @GetMapping("/rest/test")
    public String test() {
        SearchAfterRequest<Object> request = new SearchAfterRequest<>();
        EsBaseQuery query = new EsBaseQuery();
//        query.setId("228048");
        EsOrgMultiQuery orgMultiQuery=new EsOrgMultiQuery();
        orgMultiQuery.setOrgCode("10001");
        orgMultiQuery.setOrgCodeContainSub("1000010001");
        query.setEsOrgMultiQuery(orgMultiQuery);
        request.setParam(query);
//                query.setCreateName("呵呵");
        request.setPageSize(2);
        //        query.setSupOrgCode("10000");
        request.setIndex("idx_psups_supplier_library_agg_info_qa_20230207");
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
        SearchPageRequest<Object> request = new SearchPageRequest<>();
        EsBaseQuery query = new EsBaseQuery();
//                query.setId("228048");
        EsOrgMultiQuery orgMultiQuery = new EsOrgMultiQuery();
        orgMultiQuery.setOrgCode("10001");
        orgMultiQuery.setOrgCodeContainSub("10001");
//        query.setEsOrgMultiQuery(orgMultiQuery);
        query.setDynamicFieldsMap(new HashMap<>());
        DynamicSearchField field=new DynamicSearchField();
        field.setSearchType(EsSearchTypeEnum.esLike.name());
        field.setValue("门窗");
        query.getDynamicFieldsMap().put("secondCatName",field);
        field=new DynamicSearchField();
        field.setSearchType(EsSearchTypeEnum.esEquals.name());
        field.setValue("YJH20240715000005");
        query.getDynamicFieldsMap().put("planCode",field);
        request.setParam(query);
        //        query.setCreateName("呵呵");
        request.setPageSize(2);
        //        query.setSupOrgCode("10000");
        request.setIndex("alias_idx_ppls_plan_monthly_info_qa");
        SearchPageResult<Map> afterResult = esQueryService.search(request, Map.class);

        return  JsonUtils.writeAsJson(afterResult);
    }

    @GetMapping("/rest/agg")
    public String agg() {
        SearchPageRequest<Object> request = new SearchPageRequest<>();
        EsAggBaseQuery query = new EsAggBaseQuery();
        request.setParam(query);
        request.setIndex("alias_idx_ppls_plan_monthly_info_qa");

        return  JsonUtils.writeAsJson(esQueryService.agg(request));
    }
}
