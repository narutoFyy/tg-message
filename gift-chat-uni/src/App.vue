<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { registerNativePushDevice } from '@/utils/push'

onLaunch(() => {
  console.log('Gift Chat Uni Launch')
  registerNativePushDevice().catch(() => {})
})
</script>

<style lang="scss">
page {
  background: #f3f6f8;
  color: #17212b;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.page-shell {
  min-height: 100vh;
  padding: 24rpx 24rpx 172rpx;
  box-sizing: border-box;
}

.soft-page {
  background: linear-gradient(180deg, #edf4f8 0%, #f7f9fb 42%, #ffffff 100%);
}

.page-stack {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.surface-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1rpx solid rgba(136, 153, 166, 0.18);
  border-radius: 16rpx;
  box-shadow: 0 12rpx 34rpx rgba(25, 42, 62, 0.08);
}

.panel {
  @extend .surface-card;
  padding: 26rpx;
}

.eyebrow {
  color: #0088cc;
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.title {
  font-size: 38rpx;
  font-weight: 800;
  line-height: 1.25;
  color: #17212b;
}

.subtitle {
  font-size: 27rpx;
  color: #5f6f7c;
  line-height: 1.55;
}

.muted {
  color: #7d8b97;
  font-size: 24rpx;
  line-height: 1.5;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 25rpx;
  color: #3b4a54;
  font-weight: 700;
}

.field-input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(136, 153, 166, 0.34);
  border-radius: 12rpx;
  background: #ffffff;
  color: #17212b;
  font-size: 29rpx;
}

.primary-button,
.ghost-button {
  border-radius: 12rpx;
  border: none;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.2;
  padding: 22rpx 28rpx;
}

.primary-button {
  color: #ffffff;
  background: #0088cc;
  box-shadow: 0 10rpx 22rpx rgba(0, 136, 204, 0.22);
}

.ghost-button {
  color: #0088cc;
  background: #ffffff;
  border: 1rpx solid rgba(0, 136, 204, 0.32);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  padding: 10rpx 18rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.status-pill.active {
  color: #008b5c;
  background: rgba(0, 168, 132, 0.12);
}

.status-pill.paused {
  color: #b26a00;
  background: rgba(255, 178, 76, 0.18);
}

.pill-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 74rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: #ffffff;
  border: 1rpx solid rgba(136, 153, 166, 0.2);
  box-shadow: 0 8rpx 22rpx rgba(25, 42, 62, 0.06);
  color: #263642;
  font-size: 25rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 800;
  color: #17212b;
}

.tab-active-line {
  width: 50rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: #0088cc;
}
</style>
