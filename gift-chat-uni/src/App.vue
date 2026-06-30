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
  background: linear-gradient(180deg, #d8ffe7 0%, #f8fff4 26%, #ffffff 56%);
  color: #161616;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.page-shell {
  min-height: 100vh;
  padding: 28rpx 24rpx 180rpx;
  box-sizing: border-box;
}

.soft-page {
  background:
    radial-gradient(circle at top right, rgba(129, 255, 202, 0.42), transparent 30%),
    linear-gradient(180deg, #d9ffe7 0%, #f8fff6 32%, #ffffff 68%);
}

.page-stack {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.surface-card {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 34rpx;
  box-shadow: 0 18rpx 50rpx rgba(88, 156, 112, 0.12);
}

.panel {
  @extend .surface-card;
  padding: 28rpx;
}

.eyebrow {
  color: #0bc86b;
  font-size: 22rpx;
  font-weight: 700;
}

.title {
  font-size: 40rpx;
  font-weight: 800;
  line-height: 1.25;
  color: #161616;
}

.subtitle {
  font-size: 27rpx;
  color: #717171;
  line-height: 1.55;
}

.muted {
  color: #8d8d8d;
  font-size: 24rpx;
  line-height: 1.5;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 25rpx;
  color: #12b76a;
  font-weight: 700;
}

.field-input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  box-sizing: border-box;
  border: 3rpx solid #1fd58f;
  border-radius: 22rpx;
  background: #ffffff;
  color: #1b1b1b;
  font-size: 30rpx;
}

.primary-button,
.ghost-button {
  border-radius: 22rpx;
  border: none;
  font-size: 32rpx;
  font-weight: 800;
  line-height: 1.2;
  padding: 24rpx 28rpx;
}

.primary-button {
  color: #ffffff;
  background: linear-gradient(180deg, #14d86f 0%, #05c95f 100%);
}

.ghost-button {
  color: #08be61;
  background: #ffffff;
  border: 2rpx solid #16cc7d;
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
  color: #0ab95d;
  background: rgba(14, 210, 110, 0.12);
}

.status-pill.paused {
  color: #e18f38;
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
  box-shadow: 0 8rpx 24rpx rgba(16, 24, 40, 0.08);
  color: #2a2a2a;
  font-size: 25rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 800;
  color: #1e1e1e;
}

.tab-active-line {
  width: 50rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: #17d675;
}
</style>
