# 基于Qwen2的智能数据处理与分析平台

## 功能
***
+ [x] 登录注册功能
+ [X] 分布式用户验证
+ [X] 网关鉴权
+ [x] 消息队列异步化处理
+ [x] AIGC-智能文本处理
+ + [x] 文本格式转换(支持txt,doc,docx,markdown)
+ [x] AIGC-智能数据分析与可视化(支持xlsx,csv)
+ [x] AIGC-智能数据处理
+ + [x] 格式转换
+ + [x] AIGC-数据智能清洗
+ + [x] 数据合并
+ + [x] AIGC-数据智能筛选
+ [x] 积分功能

## 架构
- 服务模块：sleepingFaceBi-user（用户服务），sleepingFaceBi-chart（图表服务），sleepingFaceBi-text（文本服务）。
- 公共服务模块：sleepingFaceBi-common-ai(AI服务), sleepingFaceBi-common-commm(公共服务), sleepingFaceBi-common-mq(消息队列服务), sleepingFaceBi-common-mybatis
- 网关模块：sleepingFaceBi-gateway(网关服务)

## 技术栈
***
框架：Spring Cloud 

数据库：MySQL

网关：Spring Cloud Gateway

中间件：Redis + RabbitMq

注册中心：Nacos

RPC框架：Dubbo

插件：Sa-token（分布式用户验证），Mybatis(Plus)，Swagger（接口文档）

# 服务器部署
1. 使用服务器配置为2核4G
2. 宝塔面板
3. 详细步骤和思路笔记在项目doc目录下




