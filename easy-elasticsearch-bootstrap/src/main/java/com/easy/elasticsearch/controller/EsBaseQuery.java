package com.easy.elasticsearch.controller;

import java.io.Serializable;
import java.util.List;

import com.easy.elastic.search.annotation.EsEquals;
import com.easy.elastic.search.annotation.EsLike;
import com.easy.elastic.search.annotation.EsMulti;
import com.easy.elastic.search.annotation.EsRange;
import com.easy.elastic.search.request.EsSearchBase;
import lombok.Data;

/**
 * es查询基类
 *
 * @author: liangbaole
 * @version: 1.0.0
 * @date: 2022-08-11 20:24
 */
@Data
public class EsBaseQuery extends EsSearchBase implements Serializable {

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


    /**
     * 条件选择code列表，包含本下
     */
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


    /**
     * 创建时间
     */
    @EsRange(name = "createTime", gt = true, includeLower = true)
    private Long                  createTimeStart;
    /**
     * 创建时间
     */
    @EsRange(name = "createTime", lt = true, includeUpper = true)
    private Long                  createTimeEnd;

    @EsMulti
    private EsOrgMultiQuery       esOrgMultiQuery;
}
