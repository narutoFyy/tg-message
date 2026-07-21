<template>
  <view class="order-message-card">
    <view class="order-card-head">
      <view class="order-title-group">
        <text class="order-kicker">Gift card order</text>
        <text class="order-number">{{ order.orderNo }}</text>
      </view>
      <text :class="['order-status', `status-${order.status}`]">{{ statusLabel }}</text>
    </view>

    <view class="order-card-body">
      <image
        v-if="order.voucherImageUrl"
        class="order-voucher"
        :src="order.voucherImageUrl"
        mode="aspectFill"
        @click="$emit('preview', order.voucherImageUrl)"
      />
      <view class="order-details">
        <text class="order-card-name">{{ order.cardName }}</text>
        <text class="order-face-value">{{ order.faceValue }}</text>
        <view class="order-amount-row">
          <text class="order-amount-label">{{ order.finalLocalAmount ? 'Final payout' : 'Estimated payout' }}</text>
          <text class="order-amount">{{ displayAmount }}</text>
        </view>
      </view>
    </view>

    <view v-if="order.settledBy" class="order-settlement-note">
      <text>Settled by {{ order.settledBy }}{{ order.settledAt ? ` · ${order.settledAt}` : '' }}</text>
      <text v-if="order.manualVipPoints">VIP points +{{ order.manualVipPoints }}</text>
    </view>

    <view v-if="canManage && isActionable" class="order-actions">
      <button class="order-action secondary" @click.stop="$emit('cancel', order)">Cancel</button>
      <button class="order-action primary" @click.stop="$emit('complete', order)">Complete</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ChatOrderItem } from '@/types'

const props = withDefaults(defineProps<{
  order: ChatOrderItem
  canManage?: boolean
}>(), {
  canManage: false
})

defineEmits<{
  (event: 'preview', url: string): void
  (event: 'complete', order: ChatOrderItem): void
  (event: 'cancel', order: ChatOrderItem): void
}>()

const statusLabel = computed(() => ({
  pending: 'Pending',
  processing: 'Processing',
  completed: 'Completed',
  disputed: 'Disputed',
  canceled: 'Canceled'
}[props.order.status] || props.order.status))
const isActionable = computed(() => props.order.status === 'pending' || props.order.status === 'processing')
const displayAmount = computed(() => {
  const amount = props.order.finalLocalAmount || props.order.estimatedLocalAmount
  return amount ? `${props.order.currencyCode} ${amount}` : props.order.payoutAmount
})
</script>

<style scoped>
.order-message-card {
  width: min(420px, 76vw);
  overflow: hidden;
  border: 1px solid rgba(31, 63, 49, 0.16);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 3px 12px rgba(33, 50, 42, 0.1);
}

.order-card-head,
.order-card-body,
.order-settlement-note,
.order-actions {
  box-sizing: border-box;
}

.order-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 14px 11px;
  border-bottom: 1px solid #e8ece9;
}

.order-title-group,
.order-details,
.order-settlement-note {
  min-width: 0;
}

.order-kicker,
.order-number,
.order-card-name,
.order-face-value,
.order-settlement-note text {
  display: block;
}

.order-kicker {
  color: #728078;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}

.order-number {
  margin-top: 3px;
  color: #1e2a24;
  font-size: 14px;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.order-status {
  flex: 0 0 auto;
  padding: 4px 7px;
  border-radius: 5px;
  background: #eef1ef;
  color: #5d6962;
  font-size: 11px;
  font-weight: 800;
}

.status-processing,
.status-pending { background: #fff3d6; color: #855b00; }
.status-completed { background: #dcf3e7; color: #176844; }
.status-disputed { background: #fce8cc; color: #8b4c00; }
.status-canceled { background: #f3e3e3; color: #8d3434; }

.order-card-body {
  display: flex;
  gap: 12px;
  padding: 14px;
}

.order-voucher {
  width: 76px;
  height: 76px;
  flex: 0 0 76px;
  border-radius: 6px;
  background: #eef1ef;
}

.order-details {
  flex: 1;
}

.order-card-name {
  color: #17211c;
  font-size: 16px;
  font-weight: 800;
}

.order-face-value {
  margin-top: 3px;
  color: #67736c;
  font-size: 13px;
}

.order-amount-row {
  margin-top: 12px;
}

.order-amount-label,
.order-amount {
  display: block;
}

.order-amount-label {
  color: #7a857f;
  font-size: 11px;
}

.order-amount {
  margin-top: 2px;
  color: #002fa7;
  font-size: 18px;
  font-weight: 900;
}

.order-settlement-note {
  padding: 9px 14px;
  border-top: 1px solid #edf0ee;
  color: #59665f;
  font-size: 11px;
  line-height: 1.5;
}

.order-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 10px 14px 14px;
}

.order-action {
  height: 36px;
  margin: 0;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 800;
  line-height: 36px;
}

.order-action::after { border: 0; }
.order-action.secondary { background: #f0f2f1; color: #34423b; }
.order-action.primary { background: #176844; color: #ffffff; }

@media (max-width: 480px) {
  .order-message-card { width: min(340px, 78vw); }
  .order-voucher { width: 64px; height: 64px; flex-basis: 64px; }
}
</style>
