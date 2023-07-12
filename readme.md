# 微服务设计模式练兵场

这个项目用来个人编程实践学习微服务设计模式，多语言、多框架支持。

- [java-service](java-service/readme.md) - 微服务 Spring 实现
- [node-service](node-service/readme.md) - 微服务 NodeJS 实现
- [python-service](python-service/readme.md) - 微服务 Python 实现
- [vertx-service](vertx-service/readme.md) - 微服务 Vert.X 实现
- `db` - 数据库模型和数据
- `event` - 事件定义
- `script` - 脚本
- `integration-test` - 集成测试
- `env.json` - 环境变量
- `setup-env.ps1` - 初始化终端的环境变量
- `service.yaml` - 服务定义

基础设施 | 实现
--- | --- |
配置 | 环境变量
同步通信 | REST
存储 | 内存对象, MySQL
消息代理 | Pulsar, Kafka
测试 | restclient, py scripts


## 已实践的设计模式

- Saga 模式
- 异步请求-答复模式
- 速率限制模式

## 微服务设计模式相关资料

- 《微服务架构设计模式》- Chris Richardson
- https://learn.microsoft.com/zh-cn/azure/architecture/patterns/
- https://microservices.io/patterns/microservices.html
- https://blogs.oracle.com/developers/post/developing-saga-participant-code-for-compensating-transactions