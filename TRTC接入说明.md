# TRTC接入说明

当前项目已经加好了“视频通话壳子”：

- 前端聊天页有 `Video` 入口
- 后端有 `video_session` 记录表
- 后端有 `/api/video-sessions` 创建和状态更新接口
- 前端有 `/pages/video-call/index` 占位呼叫页

## 现在还缺什么

要接入腾讯云 `TRTC_Web`，还需要你提供下面两个配置：

- `SDKAppID`
- `SDKSecretKey`

其中：

- `SDKAppID` 可以下发给前端
- `SDKSecretKey` 只能放后端，不能写进前端代码

## 当前后端预留配置

在后端环境变量里预留了：

- `APP_VIDEO_VENDOR=trtc`
- `APP_VIDEO_TRTC_SDK_APP_ID=`

后面我们还会补：

- `APP_VIDEO_TRTC_SECRET_KEY=`

## 下一步接入计划

1. 后端增加 `UserSig` 真实生成逻辑
2. 前端安装 `trtc-sdk-v5`
3. 在 `pages/video-call/index` 里调用 TRTC SDK
4. 用 `roomId + userSig + sdkAppId` 真实入会
5. 补通话中、挂断、超时、拒接状态

## 当前接口

### 创建视频会话

`POST /api/video-sessions`

请求示例：

```json
{
  "channelType": "support",
  "channelId": "support-1"
}
```

返回里现在会给：

- `roomId`
- `sdkAppId`
- `userId`
- `userSig` 占位符
- `sdkConfigured`

说明：

当前 `userSig` 还是占位值，等你给我腾讯云配置后我再改成真实签名。
