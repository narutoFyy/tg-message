<template>
  <view class="call-card">
    <view :class="['call-icon', status]">▶</view>
    <view class="call-main">
      <view class="call-head">
        <text class="call-title">{{ title }}</text>
        <text :class="['call-status', status]">{{ statusLabel }}</text>
      </view>
      <text class="call-room">{{ room }}</text>
      <text class="call-caption">{{ caption }}</text>
      <view v-if="canAnswer || canReject || canEnter" class="call-actions">
        <button v-if="canAnswer" class="call-btn answer" @click.stop="$emit('answer')">{{ answerLabel }}</button>
        <button v-if="canReject" class="call-btn decline" @click.stop="$emit('reject')">{{ rejectLabel }}</button>
        <button v-if="canEnter" class="call-btn enter" @click.stop="$emit('enter')">{{ enterLabel }}</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import type { VideoSessionItem } from '@/types'

withDefaults(defineProps<{
  title: string
  room: string
  status: VideoSessionItem['status'] | string
  statusLabel: string
  caption?: string
  answerLabel?: string
  rejectLabel?: string
  enterLabel?: string
  canAnswer?: boolean
  canReject?: boolean
  canEnter?: boolean
}>(), {
  caption: '',
  answerLabel: 'Answer',
  rejectLabel: 'Decline',
  enterLabel: 'Enter'
})

defineEmits<{
  (event: 'answer'): void
  (event: 'reject'): void
  (event: 'enter'): void
}>()
</script>

<style scoped>
.call-card {
  display: flex;
  gap: 10px;
  min-width: min(300px, 70vw);
  max-width: 360px;
  padding: 12px;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff, #f8fbf7);
  border: 1px solid rgba(37, 99, 82, 0.14);
  box-shadow: 0 8px 24px rgba(25, 54, 42, 0.08);
  box-sizing: border-box;
}

.call-icon {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: 8px;
  background: #143d34;
  color: #ffffff;
  font-size: 14px;
  line-height: 36px;
  text-align: center;
}

.call-icon.created,
.call-icon.joining {
  background: #a06600;
}

.call-icon.ended,
.call-icon.missed,
.call-icon.rejected {
  background: #9f322c;
}

.call-main {
  flex: 1;
  min-width: 0;
}

.call-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.call-title {
  flex: 1;
  min-width: 0;
  color: #1f2d24;
  font-size: 14px;
  font-weight: 900;
  line-height: 1.35;
  word-break: break-word;
}

.call-status {
  flex: 0 0 auto;
  max-width: 96px;
  padding: 3px 7px;
  border-radius: 6px;
  background: rgba(18, 201, 107, 0.12);
  color: #0a7a44;
  font-size: 11px;
  font-weight: 900;
  line-height: 1.25;
  text-align: center;
  word-break: break-word;
}

.call-status.created,
.call-status.joining {
  background: rgba(255, 196, 77, 0.2);
  color: #8a5a00;
}

.call-status.ended,
.call-status.missed,
.call-status.rejected {
  background: rgba(255, 94, 87, 0.12);
  color: #bf3b34;
}

.call-room {
  display: block;
  margin-top: 4px;
  color: #66756c;
  font-size: 12px;
  line-height: 1.35;
  word-break: break-all;
}

.call-caption {
  display: block;
  margin-top: 6px;
  color: #42554a;
  font-size: 12px;
  line-height: 1.35;
  word-break: break-word;
}

.call-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.call-btn {
  min-width: 72px;
  height: 30px;
  padding: 0 10px;
  border: 0;
  border-radius: 6px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 900;
  line-height: 30px;
}

.call-btn.answer,
.call-btn.enter {
  background: #00a884;
}

.call-btn.decline {
  background: #ff5e57;
}
</style>
