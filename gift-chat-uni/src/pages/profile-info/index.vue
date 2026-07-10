<template>
  <view class="page-shell soft-page profile-info-page">
    <view class="page-stack">
      <view class="page-header profile-hero tone-social">
        <view class="page-header-copy">
          <text class="eyebrow">Profile</text>
          <text class="title">Personal information</text>
          <text class="subtitle">Account identity and bound bank details.</text>
        </view>
      </view>

      <view class="info-section account-section">
        <view class="section-head">
          <text class="section-title">Account</text>
          <text class="status-pill active">{{ roleLabel }}</text>
        </view>
        <view class="info-row"><text class="info-label">Username</text><text class="info-value">{{ user?.username || 'Not signed in' }}</text></view>
        <view class="info-row"><text class="info-label">Password</text><text class="info-value">Set</text></view>
        <view class="info-row"><text class="info-label">Email</text><text class="info-value">{{ user?.email || 'Not set' }}</text></view>
        <view class="info-row"><text class="info-label">Phone</text><text class="info-value">{{ user?.phone || 'Not set' }}</text></view>
      </view>

      <view class="info-section bank-section">
        <view class="section-head">
          <text class="section-title">Bank account</text>
          <text :class="['status-pill', bankAccount ? 'active' : 'paused']">{{ bankAccount ? 'Bound' : 'Not bound' }}</text>
        </view>
        <template v-if="bankAccount">
          <view class="bank-summary">
            <text class="bank-name">{{ bankAccount.bankName }}</text>
            <text class="bank-number">{{ safeBankNumber }}</text>
          </view>
          <view class="info-row"><text class="info-label">Account name</text><text class="info-value">{{ bankAccount.accountName }}</text></view>
          <view class="info-row"><text class="info-label">Country</text><text class="info-value">{{ bankAccount.country }}</text></view>
          <view class="info-row"><text class="info-label">Status</text><text class="info-value">{{ bankAccount.status }}</text></view>
          <view class="info-row"><text class="info-label">Bound at</text><text class="info-value">{{ bankAccount.createdAt }}</text></view>
        </template>
        <view v-else class="empty-bank">
          <text class="empty-title">No bank account bound</text>
          <text class="empty-copy">Bind one account before requesting a withdrawal.</text>
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

function goBindBank() { uni.navigateTo({ url: '/pages/withdraw/index' }) }
</script>

<style scoped lang="scss">
.profile-info-page { padding-bottom: 48rpx; }
.page-header-copy .eyebrow, .page-header-copy .title, .page-header-copy .subtitle { display: block; }
.page-header-copy .title { margin-top: 7rpx; }
.page-header-copy .subtitle { margin-top: 8rpx; }
.profile-hero { padding: 28rpx; align-items: center; border-bottom: 0; }
.info-section { background: #ffffff; border: 1rpx solid var(--cb-line); border-radius: 12rpx; overflow: hidden; }
.account-section { background: var(--cb-lilac); border-color: #ddd5fb; }
.bank-section { background: var(--cb-mint); border-color: #c6ead8; }
.section-head { min-height: 78rpx; padding: 0 24rpx; display: flex; align-items: center; justify-content: space-between; gap: 18rpx; border-bottom: 1rpx solid #dedfe3; }
.info-row { min-height: 76rpx; padding: 15rpx 24rpx; box-sizing: border-box; display: flex; align-items: center; justify-content: space-between; gap: 24rpx; border-bottom: 1rpx solid #dedfe3; }
.info-row:last-child { border-bottom: 0; }
.info-label { flex: 0 0 auto; color: #6f7178; font-size: 24rpx; }
.info-value { min-width: 0; color: #111111; font-size: 25rpx; font-weight: 700; text-align: right; overflow-wrap: anywhere; }
.bank-summary { padding: 24rpx; border-bottom: 1rpx solid #c6ead8; border-left: 5rpx solid var(--cb-mint-strong); background: rgba(255, 255, 255, 0.72); }
.bank-name, .bank-number { display: block; }
.bank-name { color: #111111; font-size: 25rpx; font-weight: 700; }
.bank-number { margin-top: 12rpx; color: #0f7f49; font-size: 32rpx; font-weight: 700; }
.empty-bank { padding: 30rpx 24rpx 24rpx; }
.empty-title { display: block; color: #111111; font-size: 27rpx; font-weight: 700; }
.empty-copy { display: block; margin-top: 9rpx; color: #6f7178; font-size: 24rpx; line-height: 1.4; }
.bind-button { width: 100%; margin-top: 24rpx; }
.notice-text { display: block; color: #6f7178; font-size: 23rpx; text-align: center; }
</style>
