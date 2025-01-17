# 应用说明
This is a lightweight Elasticsearch search component based on Java annotations, designed to simplify the complexities of working with Elasticsearch. Developers no longer need to worry about the intricate details of building ES query DSL strings. By simply annotating the relevant properties of Java objects based on business logic, developers can easily implement complex search functionality, significantly reducing development effort and time. 
If you're interested in contributing to the open-source project or need technical consultation, feel free to contact us via email: 382576883@qq.com.

这是一款基于Java注解的轻量级Elasticsearch搜索组件，旨在解决Elasticsearch使用难度较大的问题。开发者无需关心复杂的ES查询DSL语句的构建细节，只需根据业务需求在Java对象属性上添加相应的注解，就能轻松实现高效的搜索功能，大大减少开发难度和时间成本。
如果您对开源项目感兴趣，或需要咨询技术细节，或需要加入elastic中文社区，欢迎通过邮件与我们联系：382576883@qq.com。

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
     * equals 等于
     */
    @EsEquals(name = "_id")
    private String                id;
    /**
     * not equals 不等于
     */
    @EsNotEquals
    private String                tenantId;
    /**
     * like 模糊搜索 左右模糊可选 支持？*等特殊字符
     */
    @EsLike(name = "orgName", leftLike = true, rightLike = true)
    private String                purOrgName;
    /**
     * es 分词匹配
     */
    @EsMatch
    private String                context;
    /**
     * range 范围查询起
     */
    @EsRange(name = "createTime", gt = true, includeLower = true)
    private Long                  createTimeStart;
    /**
     * range 范围查询止
     */
    @EsRange(name = "createTime", lt = true, includeUpper = true)
    private Long                  createTimeEnd;
    /**
     * 嵌套子查询
     */
    @EsMulti
    private EsOrgMultiQuery       esOrgMultiQuery;
    /**
     * nested 
     */
    @EsNested(name = "subList")
    private EsNestedQuery         nestedQuery;
    
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
