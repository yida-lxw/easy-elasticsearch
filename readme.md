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
@Bean
public EsQueryClient esQueryClient() {
return new EsQueryClient();
}

jc2-common-dmigrate： 实现接口{DMigrationService，ExchangeStrategyService},初始化DataExchangeDealClient，示例如下：
@Bean
public DataExchangeDealClient client() {
return new DataExchangeDealClient();
}

jc2-common-utils： 开箱即用

