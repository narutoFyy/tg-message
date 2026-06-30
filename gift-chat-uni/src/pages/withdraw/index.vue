<template>
  <view class="page-shell soft-page">
    <view class="page-stack">
      <view class="panel">
        <text class="eyebrow">Withdraw</text>
        <view style="height: 12rpx"></view>
        <text class="title">Bank payout request</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">Submit bank details and the request is sent to your dedicated support agent.</text>
      </view>

      <view class="panel balance-panel">
        <text class="balance-label">Available balance</text>
        <text class="balance-value">{{ availableBalance }}</text>
      </view>

      <view class="panel form-card">
        <text class="field-label">Amount</text>
        <input v-model="form.amount" class="field-input" placeholder="₦10000" />
        <view style="height: 18rpx"></view>
        <text class="field-label">Country</text>
        <input v-model="form.country" class="field-input" placeholder="Nigeria / India / Cameroon / Ghana" />
        <view style="height: 18rpx"></view>
        <text class="field-label">Account name</text>
        <input v-model="form.accountName" class="field-input" placeholder="Account holder name" />
        <view style="height: 18rpx"></view>
        <text class="field-label">Bank name</text>
        <input v-model="form.bankName" class="field-input" placeholder="Bank name" />
        <view style="height: 18rpx"></view>
        <text class="field-label">Account number</text>
        <input v-model="form.accountNumber" class="field-input" placeholder="Account number" />
        <view style="height: 18rpx"></view>
        <text class="field-label">Contact</text>
        <input v-model="form.contact" class="field-input" placeholder="Phone or WhatsApp" />
        <view style="height: 24rpx"></view>
        <button class="primary-button" @click="submit">Submit withdrawal</button>
      </view>

      <view v-if="store.state.withdrawals.length" class="panel">
        <text class="section-title">Withdrawal records</text>
        <view v-for="item in store.state.withdrawals" :key="item.id" class="withdraw-row">
          <view>
            <text class="row-title">{{ item.requestNo }}</text>
            <text class="row-meta">{{ item.bankName }} / {{ item.accountNumber }}</text>
            <text class="row-meta">{{ item.createdAt }}</text>
          </view>
          <view class="amount-side">
            <text class="amount">{{ item.amount }}</text>
            <text :class="['status-pill', item.status === 'completed' ? 'active' : 'warning']">{{ item.status }}</text>
          </view>
        </view>
      </view>

      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'

const store = useAppStore()
const notice = ref('')
const form = reactive({
  amount: '',
  country: '',
  accountName: '',
  bankName: '',
  accountNumber: '',
  contact: ''
})

onShow(() => {
  store.bootstrap()
})

const availableBalance = computed(() => {
  const completed = store.state.transactions
    .filter((item) => item.status === 'completed')
    .reduce((sum, item) => sum + parseMoney(item.payoutAmount), 0)
  const withdrawn = store.state.withdrawals.reduce((sum, item) => sum + parseMoney(item.amount), 0)
  return `₦${Math.max(0, completed - withdrawn).toLocaleString('en-US')}`
})

function parseMoney(value: string) {
  return Number(value.replace(/[^\d.]/g, '') || '0')
}

async function submit() {
  try {
    const withdrawal = await store.createWithdrawal({
      amount: form.amount,
      country: form.country,
      accountName: form.accountName,
      bankName: form.bankName,
      accountNumber: form.accountNumber,
      contact: form.contact || undefined,
      sendChatMessage: false
    })
    uni.setStorageSync('pending-support-draft', buildWithdrawalDraft(withdrawal.requestNo))
    uni.redirectTo({ url: '/pages/support/index' })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Withdrawal failed'
  }
}

function buildWithdrawalDraft(requestNo: string) {
  return [
    `Withdrawal request ${requestNo}`,
    `Amount: ${form.amount}`,
    `Country: ${form.country}`,
    `Account: ${form.accountName}`,
    `Bank: ${form.bankName}`,
    `Number: ${form.accountNumber}`,
    form.contact ? `Contact: ${form.contact}` : ''
  ].filter(Boolean).join('\n')
}
</script>

<style scoped lang="scss">
.balance-panel {
  background: linear-gradient(180deg, #e9fff4, #f8fffb);
}

.balance-label,
.row-meta {
  display: block;
  font-size: 23rpx;
  color: #7b838c;
}

.balance-value {
  display: block;
  margin-top: 10rpx;
  font-size: 52rpx;
  font-weight: 900;
  color: #0f9b57;
}

.withdraw-row {
  padding: 18rpx 0;
  border-bottom: 1rpx solid #eef1f3;
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
}

.row-title {
  display: block;
  font-size: 28rpx;
  font-weight: 900;
}

.amount-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10rpx;
}

.amount {
  font-size: 28rpx;
  font-weight: 900;
  color: #0f9b57;
}

.notice-text {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #5d646d;
}
</style>
