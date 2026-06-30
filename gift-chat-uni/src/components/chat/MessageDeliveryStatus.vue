<template>
  <view
    :class="['delivery-status', statusClass, { clickable: failed }]"
    @click.stop="failed && $emit('retry')"
  >
    <text v-if="failed" class="delivery-retry">Retry</text>
    <text v-else class="delivery-check">{{ statusText }}</text>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ChatMessage } from '@/types'

const props = defineProps<{
  state?: ChatMessage['readState']
}>()

defineEmits<{
  (event: 'retry'): void
}>()

const failed = computed(() => props.state === 'failed')
const statusClass = computed(() => props.state || 'sent')
const statusText = computed(() => props.state === 'read' ? '✓✓' : '✓')
</script>

<style scoped>
.delivery-status {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  flex: 0 0 auto;
  min-width: 16px;
  color: #0a8f5d;
  font-size: 12px;
  line-height: 1;
  text-align: right;
  white-space: nowrap;
}

.delivery-status.read {
  color: #07845f;
}

.delivery-status.sending {
  color: #7e927c;
}

.delivery-status.failed {
  color: #d92d20;
}

.delivery-status.clickable {
  cursor: pointer;
}

.delivery-check {
  display: inline-block;
  font-weight: 800;
  letter-spacing: 0;
  white-space: nowrap;
}

.delivery-retry {
  font-size: 11px;
  font-weight: 800;
}
</style>
