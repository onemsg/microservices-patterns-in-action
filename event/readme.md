# 事件定义

> 消息队列使用 [Pulsar](https://pulsar.apache.org/)。

三种类型的消息
- **文档** - 仅包含数据的通用消息。接收者决定如何解释它。对命令式消息的回复是文档消息的一种使用场景。
- **命令** - 一条等同于 RPC 请求的消息。它指定要调用的操作及其参数。
- **事件** - 表示发送方这一端发生了重要的事件。事件通常是领域事件，表示领域对象（如 Order 或 Customer）的状态更改。

## Order Service 命令管道

Topic name `order-service-command-channel`

MessageName | Type | Description
--- | --- | ---
ApproveOrder | command | 允许订单
RejectOrder | command | 拒绝订单

## Create order saga 回复管道

Topic name `create-order-saga-reply-channel`

MessageName | Type | Description
--- | --- | ---
ConsumerVerified | replay | 消费者验证消息
ConsumerVerifiedFailed | replay | 消费者验证失败消息
TicketCreated | replay | 厨房工单创建消息
TicketCreatedFailed | replay | 厨房工单创建失败消息
CardAuthorized | replay | 信用卡验证消息
CardAuthorizedFailed | replay | 信用卡验证失败消息

## Consumer Service 命令管道

Topic name `consumer-service-command-channel`

MessageName | Type | Description
--- | --- | ---
VerifyConsumer | command | 验证消费者

## Kitchen Service 命令管道

Topic name `kitchen-service-command-channel`

MessageName | Type | Description
--- | --- | ---
CreateTicket | command | 创建厨房订单
ApproveRestaurantOrder | command | 允许商家订单
RejectRestaurantOrder | command | 拒绝商家订单

## Accounting Service 命令管道

Topic name `accounting-service-command-channel`

MessageName | Type | Description
--- | --- | ---
AuthorizedCard | command | 验证信用卡