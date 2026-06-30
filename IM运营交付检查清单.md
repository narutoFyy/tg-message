# IM 运营交付检查清单

## 每日检查

- 后端健康：`/api/rates`、登录、客服会话列表、发送客服消息。
- Redis：`docker compose ps redis`，确认健康检查为 healthy。
- Tencent Chat：检查生产环境 `APP_TENCENT_CHAT_ENABLED`、`APP_TENCENT_CHAT_SDK_APP_ID`、管理员 UserSig/SecretKey。
- 失败镜像：管理员调用 `/api/admin/im/tencent-mirror-failures`，如有失败再调用 `/api/admin/im/tencent-mirror-failures/retry`。
- 移动推送：抽测 iOS/Android 后台点击通知能进入正确客服会话。

## 上线前压测建议

- 单用户连续发送：超过 20 条/分钟应被限流。
- 断线重连：关闭网络后发送、恢复网络后通过 `serverSeq` 补齐消息。
- 多实例：两台 backend 连接同一 Redis，分别连接客服和用户，验证消息可跨实例到达。
- 腾讯短故障：临时关闭腾讯配置或模拟错误，确认本地消息仍发送成功，镜像状态进入 FAILED/SKIPPED。

## 敏感信息

- 锁屏通知只放泛化文案，不放卡密、金额、银行卡、订单敏感数据。
- TRTC SecretKey、Tencent Chat SecretKey、JWT Secret 只放后端环境变量，不进前端。

## 客户交付提醒

- iOS 真机推送必须有正确 APNs 配置和 bundle id。
- Android 真机推送要按目标市场配置厂商通道。
- H5 不支持原生离线推送，只支持 WebSocket 在线消息和重连补偿。
