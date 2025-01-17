package com.easy.elasticsearch.controller;

import com.easy.elastic.search.annotation.EsEquals;
import com.easy.elastic.search.annotation.EsLike;
import com.easy.elastic.search.annotation.EsMulti;
import com.easy.elastic.search.annotation.EsRange;
import com.easy.elastic.search.request.EsBaseSearchParam;
import lombok.Data;

import java.io.Serializable;

/**
 * es查询基类
 *
 * @author: liangbaole
 * @version: 1.0.0
 * @date: 2024-08-11 20:24
 */
@Data
public class DefaultEsBaseSearchParam extends EsBaseSearchParam implements Serializable {

    /**
     * 同主UK
     */
    @EsEquals(name = "_id")
    private String                id;

    /**
     * 租户
     */
    @EsEquals
    private String                tenantId;



    @EsMulti
    private EsOrgMultiQuery       conditionOrgQuery;

    /**
     * 名称
     */
    @EsLike(name = "orgName", leftLike = true, rightLike = true)
    private String                purOrgName;

    /**
     * 供应商id
     */
    @EsEquals
    private Long                  supCompanyId;


    @EsRange(name = "createTime", gt = true, includeLower = true)
    private Long                  createTimeStart;

    @EsRange(name = "createTime", lt = true, includeUpper = true)
    private Long                  createTimeEnd;

    @EsMulti
    private EsOrgMultiQuery       esOrgMultiQuery;
}
