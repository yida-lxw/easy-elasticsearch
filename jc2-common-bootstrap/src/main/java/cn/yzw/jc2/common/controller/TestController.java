package cn.yzw.jc2.common.controller;

import cn.yzw.infra.component.utils.JsonUtils;
import cn.yzw.jc2.common.search.client.EsQueryClient;
import cn.yzw.jc2.common.search.request.SearchAfterRequest;
import cn.yzw.jc2.common.search.request.SearchPageRequest;
import cn.yzw.jc2.common.search.result.EsAggregationResult;
import cn.yzw.jc2.common.search.result.SearchAfterResult;
import cn.yzw.jc2.common.search.result.SearchPageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
public class TestController {

    @Autowired
    private EsQueryClient esQueryService;

    @GetMapping("/rest/test")
    public String test() {
        SearchAfterRequest<Object> request = new SearchAfterRequest<>();
        EsBaseQuery query = new EsBaseQuery();
        request.setTenantId("cscec");
        query.setId("cscec_228048");
        EsOrgMultiQuery orgMultiQuery=new EsOrgMultiQuery();
        orgMultiQuery.setOrgCode("10001");
        orgMultiQuery.setOrgCodeContainSub("1000010001");
        query.setEsOrgMultiQuery(orgMultiQuery);
        request.setParam(query);
        //        query.setCreateName("呵呵");
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
        request.setTenantId("cscec");
        //        query.setId("cscec_228048");
        EsOrgMultiQuery orgMultiQuery = new EsOrgMultiQuery();
        orgMultiQuery.setOrgCode("10001");
        orgMultiQuery.setOrgCodeContainSub("1000010001");
        query.setEsOrgMultiQuery(orgMultiQuery);
        request.setParam(query);
        //        query.setCreateName("呵呵");
        request.setPageSize(2);
        //        query.setSupOrgCode("10000");
        request.setIndex("idx_ppls_plan_monthly_info_qa");
        SearchPageResult<Map> afterResult = esQueryService.search(request, Map.class);

        return JsonUtils.writeAsJson(afterResult);
    }

    @GetMapping("/rest/agg")
    public String agg() {
        SearchPageRequest<Object> request = new SearchPageRequest<>();
        EsAggBaseQuery query = new EsAggBaseQuery();
        request.setParam(query);
        request.setIndex("idx_ppls_plan_project_info_qa");

        return JsonUtils.writeAsJson(esQueryService.agg(request));
    }
}
