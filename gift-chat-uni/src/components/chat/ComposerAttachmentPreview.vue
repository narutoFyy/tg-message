<template>
  <view v-if="attachment" :class="['composer-preview', attachment.status]">
    <image class="preview-thumb" :src="attachment.url" mode="aspectFill" />
    <view class="preview-body">
      <view class="preview-title-row">
        <text class="preview-title">{{ title }}</text>
        <text v-if="attachment.kind === 'gif'" class="preview-kind">GIF</text>
      </view>
      <text class="preview-status">{{ statusText }}</text>
    </view>
    <button
      v-if="attachment.status === 'failed'"
      class="preview-action retry"
      @click="$emit('retry', attachment.id)"
    >
      Retry
    </button>
    <button class="preview-action clear" aria-label="Clear attachment" @click="$emit('clear', attachment.id)">×</button>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ComposerAttachment } from './useComposerAttachments'

const props = defineProps<{
  attachment: ComposerAttachment | null
}>()

defineEmits<{
  (event: 'retry', id: string): void
  (event: 'clear', id: string): void
}>()

const title = computed(() => props.attachment?.name || 'Attachment ready')
const statusText = computed(() => {
  if (!props.attachment) return ''
  if (props.attachment.status === 'uploading') return 'Sending...'
  if (props.attachment.status === 'failed') return props.attachment.error || 'Send failed'
  return 'Ready to send'
})
</script>

<style scoped>
.composer-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 10px 16px 0;
  padding: 10px;
  border: 1px solid rgba(90, 123, 89, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-sizing: border-box;
  box-shadow: 0 8px 22px rgba(31, 58, 42, 0.08);
}

.composer-preview.failed {
  background: #fff7f6;
}

.preview-thumb {
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  border-radius: 8px;
  background: #eef2ec;
}

.preview-body {
  flex: 1;
  min-width: 0;
}

.preview-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.preview-title {
  min-width: 0;
  overflow: hidden;
  color: #23352b;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-kind {
  flex: 0 0 auto;
  padding: 2px 6px;
  border-radius: 5px;
  background: #143d34;
  color: #ffffff;
  font-size: 10px;
  font-weight: 900;
  line-height: 1.2;
}

.preview-status {
  display: block;
  margin-top: 3px;
  color: #6f8069;
  font-size: 12px;
  line-height: 1.3;
}

.composer-preview.failed .preview-status {
  color: #c24132;
}

.preview-action {
  width: auto;
  height: 30px;
  margin: 0;
  padding: 0 10px;
  border: 0;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 900;
  line-height: 30px;
}

.preview-action.retry {
  background: #00a884;
  color: #ffffff;
}

.preview-action.clear {
  min-width: 30px;
  padding: 0 8px;
  background: #edf1eb;
  color: #32463a;
  font-size: 18px;
}

@media (max-width: 768px) {
  .composer-preview {
    margin: 8px 10px 0;
  }
}
</style>
