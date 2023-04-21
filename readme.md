# 应用说明
通用组件沉淀，避免多项目来回copy,出现问题，也避免一个项目踩坑修复后，其他项目跟着踩坑，组件能力增强，接入该组件的应用组件能力都跟着增强

## 模块职责

| 模块                          | 说明    |
|:----------------------------|:------|
| `jc2-common-bootstrap`      | 启动测试  |
| `jc2-common-dmigrate`       | 数据迁移  |
| `jc2-common-elastic-search` | es 检索 |
| `jc2-common-utils`          | 工具包   |

## 接入说明

jc2-common-elastic-search： 项目正常配置es的地址，初始化EsQueryClient。示例如下：
```java

public class BeanConfig{
    @Bean
    public EsQueryClient esQueryClient() {
        return new EsQueryClient();
    } 
}

```

jc2-common-dmigrate： 实现接口{DMigrationService，ExchangeStrategyService},初始化DataExchangeDealClient，示例如下：
```java
public class BeanConfig{
    @Bean
    public DataExchangeDealClient client() {
        return new DataExchangeDealClient();
    }
}

```
jc2-common-utils： 开箱即用

## 版本说明
jc2-common-dmigrate（数据迁移）

| 版本                          | 说明         |
|:----------------------------|:-----------|
| `1.0.0-SNAPSHOT`            | 支持数据迁移基础功能 |



jc2-common-elastic-search（es 检索）

| 版本                          | 说明                    |
|:----------------------------|:----------------------|
| `1.0.0-SNAPSHOT`            | 支持注解方式检索es,暂不支持注解方式聚合 |


jc2-common-utils（工具包）

| 版本      | 说明                                |
|:--------|:----------------------------------|
| `1.0.0` | 1.json工具解析日期增强<br/>2.多线程支持父子传递上下文 |
