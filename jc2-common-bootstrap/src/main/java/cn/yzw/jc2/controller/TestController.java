package cn.yzw.jc2.controller;

import cn.yzw.infra.component.utils.JsonUtils;
import cn.yzw.jc2.request.SearchAfterRequest;
import cn.yzw.jc2.result.SearchAfterResult;
import cn.yzw.jc2.client.EsQueryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
}
