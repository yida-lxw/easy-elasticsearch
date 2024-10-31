# 应用说明
这是一款基于java注解的轻量级的elasticsearch搜索的组件，解决了ES搜索入门难的问题，
开发人员不需要关注es查询DSL语句拼接的细节，只要能理清业务逻辑，基于java对象属性，标注相应的注解，即可解决复杂搜索
一天的工作，1小时搞定
开源不易，如有感兴趣参与开源研发，或者技术使用细节咨询，请微信联系开发人员，gechulouxi，或者发email: liangbaole@qq.com

## 模块职责

| 模块                              | 说明    |
|:--------------------------------|:------|
| easy-elasticsearch-bootstrap    | 启动测试  |
| easy-elasticsearch-search       | es 检索 |

## 接入说明
###1.引入jar包
<dependency>
<groupId>com.easy.elasticsearch</groupId>
<artifactId>easy-elasticsearch-client</artifactId>
<version>1.0.0-SNAPSHOT</version>
</dependency>

###2.项目正常配置es的地址
spring.elasticsearch.rest.uris=http://127.0.0.1:9200,http://127.0.0.2:9200
spring.elasticsearch.rest.username=elastic
spring.elasticsearch.rest.password=elastic

###3.初始化EsQueryClient。示例如下：
```java

public class BeanConfig{
    @Bean
    public EsQueryClient esQueryClient() {
        return new EsQueryClient();
    } 
}

```
###4. 使用示例
```java
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
    
    public String search() {
        SearchPageRequest<Object> request = new SearchPageRequest<>();
        EsBaseQuery query = new EsBaseQuery();
        query.setId("123");
        EsOrgMultiQuery orgMultiQuery = new EsOrgMultiQuery();
        orgMultiQuery.setOrgCode("10005767661");
        orgMultiQuery.setOrgCodeContainSub("13453460001");
        request.setParam(query);
        request.setPageSize(20);
        request.setIndex("alias_idx_test");
        SearchPageResult<Map> afterResult = esQueryService.search(request, Map.class);
        return  JsonUtils.writeAsJson(afterResult);
    }
}
```
