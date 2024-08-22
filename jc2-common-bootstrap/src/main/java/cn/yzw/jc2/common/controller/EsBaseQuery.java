package cn.yzw.jc2.common.controller;

import java.io.Serializable;
import java.util.List;

import cn.yzw.jc2.common.search.annotation.EsEquals;
import cn.yzw.jc2.common.search.annotation.EsLike;
import cn.yzw.jc2.common.search.annotation.EsMulti;
import cn.yzw.jc2.common.search.annotation.EsRange;
import cn.yzw.jc2.common.search.request.EsSearchBase;
import lombok.Data;

/**
 * es查询基类
 *
 * @author: zhangzhibao
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
     * 数据权限：组织+品类
     */
    @EsMulti
    private List<EsOrgMultiQuery> dataAuthCheckList;

    /**
     * 条件选择的组织code列表，包含本下
     */
    @EsMulti
    private EsOrgMultiQuery       conditionOrgQuery;

    /**
     * 采购商名称
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
