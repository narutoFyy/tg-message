<template>
  <view class="page-shell soft-page profile-info-page">
    <view class="page-stack">
      <view class="panel hero-panel">
        <text class="eyebrow">Personal Information</text>
        <view style="height: 12rpx"></view>
        <text class="title">Account details</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">View your account profile and bound bank account.</text>
      </view>

      <view class="panel info-panel">
        <view class="section-head">
          <text class="section-title">Account</text>
          <text class="status-pill active">{{ roleLabel }}</text>
        </view>

        <view class="info-row">
          <text class="info-label">Username</text>
          <text class="info-value">{{ user?.username || 'Not signed in' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">Password</text>
          <text class="info-value">Set</text>
        </view>
        <view class="info-row">
          <text class="info-label">Email</text>
          <text class="info-value">{{ user?.email || 'Not set' }}</text>
        </view>
        <view class="info-row last">
          <text class="info-label">Phone</text>
          <text class="info-value">{{ user?.phone || 'Not set' }}</text>
        </view>
      </view>

      <view class="panel info-panel">
        <view class="section-head">
          <text class="section-title">Bank Account</text>
          <text :class="['status-pill', bankAccount ? 'active' : 'paused']">
            {{ bankAccount ? 'Bound' : 'Not bound' }}
          </text>
        </view>

        <view v-if="bankAccount">
          <view class="bank-card">
            <text class="bank-name">{{ bankAccount.bankName }}</text>
            <text class="bank-number">{{ safeBankNumber }}</text>
          </view>

          <view class="info-row">
            <text class="info-label">Account name</text>
            <text class="info-value">{{ bankAccount.accountName }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Country</text>
            <text class="info-value">{{ bankAccount.country }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Status</text>
            <text class="info-value">{{ bankAccount.status }}</text>
          </view>
          <view class="info-row last">
            <text class="info-label">Bound at</text>
            <text class="info-value">{{ bankAccount.createdAt }}</text>
          </view>
        </view>

        <view v-else class="empty-bank">
          <text class="empty-title">No bank account bound</text>
          <text class="empty-copy">Bind one bank account before sending a withdrawal request.</text>
          <button class="primary-button bind-button" @click="goBindBank">Bind bank account</button>
        </view>
      </view>

      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { fetchMyBankAccount } from '@/utils/api'
import type { BankAccountItem } from '@/types'

const store = useAppStore()
const bankAccount = ref<BankAccountItem | null>(null)
const notice = ref('')

onShow(() => {
  store.bootstrap()
  store.refreshCurrentAccount().catch(() => undefined)
  refreshBankAccount()
})

const user = computed(() => store.state.currentUser)
const roleLabel = computed(() => user.value?.roleCode || 'USER')
const safeBankNumber = computed(() => bankAccount.value?.maskedAccountNumber || '****')

async function refreshBankAccount() {
  notice.value = ''
  try {
    bankAccount.value = await fetchMyBankAccount()
  } catch (error) {
    bankAccount.value = null
    notice.value = error instanceof Error ? error.message : 'Bank account unavailable.'
  }
}

function goBindBank() {
  uni.navigateTo({ url: '/pages/withdraw/index' })
}
</script>

<style scoped lang="scss">
.profile-info-page {
  min-height: 100vh;
}

.hero-panel {
  background:
    linear-gradient(135deg, rgba(217, 255, 231, 0.96), rgba(255, 255, 255, 0.9)),
    #ffffff;
  border: 1rpx solid rgba(0, 136, 204, 0.12);
}

.info-panel {
  padding-top: 30rpx;
  padding-bottom: 30rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.info-row {
  min-height: 78rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eef1f3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  box-sizing: border-box;
}

.info-row.last {
  border-bottom: 0;
}

.info-label {
  font-size: 25rpx;
  color: #6f7a86;
  flex: 0 0 auto;
}

.info-value {
  min-width: 0;
  font-size: 28rpx;
  font-weight: 800;
  color: #171717;
  text-align: right;
  overflow-wrap: anywhere;
}

.bank-card {
  margin: 4rpx 0 12rpx;
  min-height: 150rpx;
  border-radius: 14rpx;
  padding: 26rpx;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), transparent 42%),
    linear-gradient(180deg, #17212b, #223241);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-sizing: border-box;
}

.bank-name {
  display: block;
  font-size: 30rpx;
  font-weight: 900;
}

.bank-number {
  display: block;
  margin-top: 28rpx;
  font-size: 38rpx;
  font-weight: 900;
  letter-spacing: 0;
}

.empty-bank {
  padding: 8rpx 0 4rpx;
}

.empty-title {
  display: block;
  font-size: 31rpx;
  font-weight: 900;
  color: #171717;
}

.empty-copy {
  display: block;
  margin-top: 10rpx;
  font-size: 25rpx;
  color: #6f7a86;
  line-height: 1.4;
}

.bind-button {
  margin-top: 24rpx;
}

.notice-text {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #5d646d;
}
</style>
