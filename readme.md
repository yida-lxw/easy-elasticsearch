# 应用说明
This is a lightweight elasticsearch search component based on Java annotations, which solves the problem of difficulty in getting started with ES search. Developers do not need to pay attention to the details of ES query DSL statement concatenation. As long as they can clarify the business logic, annotate the corresponding annotations based on Java object properties, they can solve the complex search work of a day. It is not easy to complete open source in 1 hour. If you are interested in participating in open source research and development or consulting on technical usage details, please send an email: 382576883@qq.com

这是一款基于java注解的轻量级的elasticsearch搜索的组件，解决了ES搜索入门难的问题，
开发人员不需要关注es查询DSL语句拼接的细节，只要能理清业务逻辑，基于java对象属性，标注相应的注解，即可解决复杂搜索
一天的工作，1小时搞定
开源不易，如有感兴趣参与开源研发，或者技术使用细节咨询，请发email: 382576883@qq.com

## 模块职责

| 模块                              | 说明    |
|:--------------------------------|:------|
| easy-elasticsearch-bootstrap    | start or test  |
| easy-elasticsearch-search       | seach client |

## instructions 接入说明
### 1.jar
```xml
<dependency>
<groupId>com.easy.elasticsearch</groupId>
<artifactId>easy-elasticsearch-client</artifactId>
<version>1.0.0-SNAPSHOT</version>
</dependency>
```
### 2.项目正常配置es的地址 es config
```properties
spring.elasticsearch.rest.uris= http://127.0.0.1:9200,http://127.0.0.2:9200
spring.elasticsearch.rest.username= elastic
spring.elasticsearch.rest.password= elastic
```
### 3.init bean: EsQueryClient
```java

public class BeanConfig{
    @Bean
    public EsQueryClient esQueryClient() {
        return new EsQueryClient();
    } 
}

```
### 4. Example 示例
```java
@Data
public class EsSearchQuery implements Serializable {

    /**
     * equals
     */
    @EsEquals(name = "_id")
    private String                id;

    /**
     * equals
     */
    @EsEquals
    private String                tenantId;
    

    /**
     * like
     */
    @EsLike(name = "orgName", leftLike = true, rightLike = true)
    private String                purOrgName;

    /**
     * equals
     */
    @EsEquals
    private Long                  supCompanyId;


    /**
     * range
     */
    @EsRange(name = "createTime", gt = true, includeLower = true)
    private Long                  createTimeStart;
    /**
     * range
     */
    @EsRange(name = "createTime", lt = true, includeUpper = true)
    private Long                  createTimeEnd;

    @EsMulti
    private EsOrgMultiQuery       esOrgMultiQuery;
    
    public String search() {
        SearchPageRequest<Object> request = new SearchPageRequest<>();
        EsSearchQuery query = new EsSearchQuery();
        query.setId("123");
        EsOrgMultiQuery orgMultiQuery = new EsOrgMultiQuery();
        orgMultiQuery.setCode("10005767661");
        orgMultiQuery.setOrgContainSub("13453460001");
        request.setParam(query);
        request.setPageSize(20);
        request.setIndex("alias_idx_test");
        SearchPageResult<Map> afterResult = esQueryService.search(request, Map.class);
        return  JsonUtils.writeAsJson(afterResult);
    }
}
```
