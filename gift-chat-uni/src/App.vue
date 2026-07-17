<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { registerNativePushDevice } from '@/utils/push'

onLaunch(() => {
  registerNativePushDevice().catch(() => {})
})
</script>

<style lang="scss">
:root {
  --cb-ink: #17212b;
  --cb-muted: #667483;
  --cb-faint: #96a1ab;
  --cb-line: #dce3ea;
  --cb-line-strong: #c7d1da;
  --cb-surface: #ffffff;
  --cb-canvas: #f3f6fa;
  --cb-accent: #2368c4;
  --cb-accent-soft: #eaf4ff;
  --cb-navy: #172633;
  --cb-navy-soft: #e8eef3;
  --cb-sky: #eaf4ff;
  --cb-sky-strong: #2f80ed;
  --cb-mint: #ddf7ea;
  --cb-mint-strong: #20b86a;
  --cb-amber: #fff3d6;
  --cb-amber-strong: #e3a72f;
  --cb-coral: #ffe7e3;
  --cb-coral-strong: #df6658;
  --cb-lilac: #f0ecff;
  --cb-lilac-strong: #6f5bd3;
  --cb-success: #168653;
  --cb-warning: #9a6500;
  --cb-danger: #bd3f35;
}

page {
  background: var(--cb-canvas);
  color: var(--cb-ink);
  font-family: Helvetica, Arial, "PingFang SC", "Microsoft YaHei", sans-serif;
  letter-spacing: 0;
}

.page-shell {
  min-height: 100vh;
  min-height: 100dvh;
  padding: calc(32rpx + constant(safe-area-inset-top)) 28rpx calc(152rpx + constant(safe-area-inset-bottom));
  padding: calc(32rpx + env(safe-area-inset-top)) 28rpx calc(152rpx + env(safe-area-inset-bottom));
  padding-left: calc(28rpx + constant(safe-area-inset-left));
  padding-left: calc(28rpx + env(safe-area-inset-left));
  padding-right: calc(28rpx + constant(safe-area-inset-right));
  padding-right: calc(28rpx + env(safe-area-inset-right));
  box-sizing: border-box;
}

.soft-page {
  background: var(--cb-canvas);
}

.page-stack {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  width: 100%;
  max-width: 960rpx;
  margin: 0 auto;
}

.surface-card {
  background: var(--cb-surface);
  border: 1rpx solid var(--cb-line);
  border-radius: 12rpx;
  box-shadow: 0 8rpx 24rpx rgba(34, 54, 74, 0.05);
}

.panel {
  @extend .surface-card;
  padding: 28rpx;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
  padding: 10rpx 0 18rpx;
  border-bottom: 1rpx solid var(--cb-line-strong);
}

.page-header-copy {
  min-width: 0;
}

.page-header-action {
  flex: 0 0 auto;
}

.tone-market,
.tone-finance,
.tone-reward,
.tone-risk,
.tone-social,
.tone-vip {
  border: 1rpx solid transparent;
  border-radius: 12rpx;
}

.tone-market {
  background: var(--cb-sky);
  border-color: #cfe4fb;
}

.tone-finance,
.tone-social {
  background: var(--cb-mint);
  border-color: #c6ead8;
}

.tone-reward {
  background: var(--cb-amber);
  border-color: #f0daa4;
}

.tone-risk {
  background: var(--cb-coral);
  border-color: #f1c8c2;
}

.tone-vip {
  background: var(--cb-navy);
  border-color: var(--cb-navy);
  color: #ffffff;
}

.tone-vip .title,
.tone-vip .section-title {
  color: #ffffff;
}

.tone-vip .subtitle,
.tone-vip .muted {
  color: rgba(255, 255, 255, 0.72);
}

.list-section {
  overflow: hidden;
  background: var(--cb-surface);
  border-top: 1rpx solid var(--cb-line);
  border-bottom: 1rpx solid var(--cb-line);
}

.list-row {
  position: relative;
  min-height: 96rpx;
  padding: 20rpx 28rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 22rpx;
  border-bottom: 1rpx solid var(--cb-line);
  background: var(--cb-surface);
}

.list-row:last-child {
  border-bottom: 0;
}

.list-row.active::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 6rpx;
  background: var(--cb-accent);
}

.eyebrow {
  color: var(--cb-accent);
  font-size: 21rpx;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0;
}

.title {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.2;
  color: var(--cb-ink);
}

.subtitle {
  font-size: 27rpx;
  color: var(--cb-muted);
  line-height: 1.5;
}

.muted {
  color: var(--cb-muted);
  font-size: 24rpx;
  line-height: 1.5;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 25rpx;
  color: var(--cb-ink);
  font-weight: 700;
}

.field-input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  box-sizing: border-box;
  border: 1rpx solid var(--cb-line-strong);
  border-radius: 6rpx;
  background: var(--cb-surface);
  color: var(--cb-ink);
  font-size: 29rpx;
}

.field-input:focus {
  border-color: var(--cb-accent);
}

.primary-button,
.ghost-button {
  min-height: 88rpx;
  border-radius: 6rpx;
  border: 1rpx solid transparent;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.2;
  padding: 20rpx 28rpx;
  box-sizing: border-box;
}

.primary-button::after,
.ghost-button::after {
  border: 0;
}

.primary-button {
  color: #ffffff;
  background: var(--cb-accent);
  box-shadow: none;
}

.ghost-button {
  color: var(--cb-accent);
  background: var(--cb-surface);
  border-color: var(--cb-line-strong);
}

.primary-button[disabled],
.ghost-button[disabled] {
  color: var(--cb-faint);
  background: #efeff1;
  border-color: #efeff1;
  opacity: 1;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4rpx;
  padding: 8rpx 14rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.status-pill.active {
  color: var(--cb-success);
  background: #eaf6f0;
}

.status-pill.paused {
  color: var(--cb-warning);
  background: #fff4df;
}

.pill-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 74rpx;
  padding: 0 28rpx;
  border-radius: 6rpx;
  background: var(--cb-surface);
  border: 1rpx solid var(--cb-line);
  box-shadow: none;
  color: var(--cb-ink);
  font-size: 25rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--cb-ink);
}

.tab-active-line {
  width: 100%;
  height: 4rpx;
  border-radius: 0;
  background: var(--cb-accent);
}

@media (min-width: 768px) {
  .page-shell {
    padding-left: 48rpx;
    padding-right: 48rpx;
  }

  .page-stack {
    max-width: 960px;
  }
}
</style>
