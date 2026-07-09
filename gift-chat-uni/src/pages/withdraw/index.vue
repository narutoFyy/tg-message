<template>
  <view class="page-shell soft-page">
    <view class="page-stack">
      <view class="panel">
        <text class="eyebrow">提现</text>
        <view style="height: 12rpx"></view>
        <text class="title">{{ lotteryRecordId ? '中奖提现申请' : '银行提现申请' }}</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">每个用户只能绑定一张银行卡，绑定后提现请求会发送给专属客服处理。</text>
      </view>

      <view v-if="!lotteryRecordId" class="panel balance-panel">
        <text class="balance-label">可提现余额</text>
        <text class="balance-value">{{ availableBalance }}</text>
      </view>

      <view class="panel form-card">
        <view class="section-head">
          <view>
            <text class="section-title">银行账户</text>
            <text class="row-meta">{{ bankAccount ? '已绑定银行卡' : '请先绑定一张银行卡' }}</text>
          </view>
          <text v-if="bankAccount" class="status-pill active">已绑定</text>
        </view>

        <view v-if="bankAccount" class="bound-bank">
          <text class="row-title">{{ bankAccount.bankName }}</text>
          <text class="row-meta">{{ bankAccount.accountName }} / {{ bankAccount.maskedAccountNumber }}</text>
          <text class="row-meta">{{ bankAccount.country }} / {{ bankAccount.createdAt }}</text>
        </view>

        <view v-else>
          <text class="field-label">国家/地区</text>
          <input v-model="form.country" class="field-input" placeholder="Nigeria / India / Ghana" />
          <view style="height: 18rpx"></view>
          <text class="field-label">持卡人姓名</text>
          <input v-model="form.accountName" class="field-input" placeholder="请输入持卡人姓名" />
          <view style="height: 18rpx"></view>
          <text class="field-label">银行名称</text>
          <input v-model="form.bankName" class="field-input" placeholder="请输入银行名称" />
          <view style="height: 18rpx"></view>
          <text class="field-label">银行卡号</text>
          <input v-model="form.accountNumber" class="field-input" placeholder="请输入银行卡号" />
          <view style="height: 24rpx"></view>
          <button class="primary-button" @click="bindAccount">绑定银行账户</button>
        </view>
      </view>

      <view class="panel form-card">
        <text class="section-title">{{ lotteryRecordId ? '申请中奖提现' : '提交提现请求' }}</text>
        <view style="height: 18rpx"></view>
        <view v-if="!lotteryRecordId">
          <text class="field-label">提现金额</text>
          <input v-model="form.amount" class="field-input" placeholder="请输入提现金额" />
          <view style="height: 18rpx"></view>
        </view>
        <text class="field-label">联系方式</text>
        <input v-model="form.contact" class="field-input" placeholder="手机号或 WhatsApp" />
        <view style="height: 24rpx"></view>
        <button class="primary-button" :disabled="!bankAccount" @click="submit">
          {{ lotteryRecordId ? '申请中奖提现' : '提交提现请求' }}
        </button>
        <text v-if="!bankAccount" class="form-hint">请先绑定银行账户，再提交提现请求。</text>
      </view>

      <view v-if="store.state.withdrawals.length" class="panel">
        <text class="section-title">提现记录</text>
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
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { bindBankAccount, fetchMyBankAccount, requestLotteryWithdrawal } from '@/utils/api'
import type { BankAccountItem } from '@/types'

const store = useAppStore()
const notice = ref('')
const lotteryRecordId = ref('')
const bankAccount = ref<BankAccountItem | null>(null)
const form = reactive({
  amount: '',
  country: '',
  accountName: '',
  bankName: '',
  accountNumber: '',
  contact: ''
})

onLoad((query) => {
  lotteryRecordId.value = typeof query?.lotteryRecordId === 'string' ? query.lotteryRecordId : ''
})

onShow(() => {
  store.bootstrap()
  refreshBankAccount()
})

const availableBalance = computed(() => {
  const completed = store.state.transactions
    .filter((item) => item.status === 'completed')
    .reduce((sum, item) => sum + parseMoney(item.payoutAmount), 0)
  const withdrawn = store.state.withdrawals.reduce((sum, item) => sum + parseMoney(item.amount), 0)
  return `NGN ${Math.max(0, completed - withdrawn).toLocaleString('en-US')}`
})

function parseMoney(value: string) {
  return Number(value.replace(/[^\d.]/g, '') || '0')
}

async function bindAccount() {
  try {
    if (!form.country.trim() || !form.accountName.trim() || !form.bankName.trim() || !form.accountNumber.trim()) {
      notice.value = '请完整填写银行卡信息。'
      return
    }
    bankAccount.value = await bindBankAccount({
      country: form.country.trim(),
      accountName: form.accountName.trim(),
      bankName: form.bankName.trim(),
      accountNumber: form.accountNumber.trim()
    })
    notice.value = lotteryRecordId.value ? '银行卡已绑定，现在可以申请中奖提现。' : '银行卡已绑定。'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '绑定银行卡失败'
  }
}

async function submit() {
  try {
    if (!bankAccount.value) {
      notice.value = '请先绑定银行账户。'
      return
    }
    if (!lotteryRecordId.value && !form.amount.trim()) {
      notice.value = '请输入提现金额。'
      return
    }
    const withdrawal = lotteryRecordId.value
      ? await requestLotteryWithdrawal(lotteryRecordId.value)
      : await store.createWithdrawal({
          amount: form.amount,
          country: bankAccount.value.country,
          accountName: bankAccount.value.accountName,
          bankName: bankAccount.value.bankName,
          accountNumber: bankAccount.value.accountNumber,
          contact: form.contact || undefined,
          sendChatMessage: true
        })
    uni.setStorageSync('pending-support-draft', buildWithdrawalDraft(withdrawal.requestNo))
    await store.bootstrap().catch(() => undefined)
    uni.redirectTo({ url: '/pages/support/index' })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '提现申请失败'
  }
}

async function refreshBankAccount() {
  try {
    bankAccount.value = await fetchMyBankAccount()
  } catch {
    bankAccount.value = null
  }
}

function buildWithdrawalDraft(requestNo: string) {
  return [
    lotteryRecordId.value ? `Lottery withdrawal request ${requestNo}` : `Withdrawal request ${requestNo}`,
    lotteryRecordId.value ? '' : `Amount: ${form.amount}`,
    `Country: ${bankAccount.value?.country || form.country}`,
    `Account: ${bankAccount.value?.accountName || form.accountName}`,
    `Bank: ${bankAccount.value?.bankName || form.bankName}`,
    `Number: ${bankAccount.value?.maskedAccountNumber || form.accountNumber}`,
    form.contact ? `Contact: ${form.contact}` : ''
  ].filter(Boolean).join('\n')
}
</script>

<style scoped lang="scss">
.balance-panel {
  background: #ffffff;
  border: 1rpx solid rgba(136, 153, 166, 0.18);
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
  color: #0088cc;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 18rpx;
}

.bound-bank {
  padding: 18rpx;
  border-radius: 8rpx;
  background: #f7fafb;
  border: 1rpx solid rgba(136, 153, 166, 0.16);
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
  color: #0088cc;
}

.notice-text {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #5d646d;
}

.form-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 23rpx;
  color: #6f7a86;
}
</style>
