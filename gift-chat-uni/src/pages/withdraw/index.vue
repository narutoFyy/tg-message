<template>
  <view class="page-shell soft-page withdraw-page">
    <view class="page-stack">
      <view class="page-header withdraw-hero tone-finance">
        <view class="page-header-copy">
          <text class="eyebrow">Wallet</text>
          <text class="title">{{ lotteryRecordId ? 'Claim lottery prize' : 'Withdraw funds' }}</text>
          <text class="subtitle">Withdrawals are reviewed by your support team using your bound bank account.</text>
        </view>
      </view>

      <view v-if="!lotteryRecordId" class="balance-panel tone-finance">
        <text class="balance-label">Available to withdraw</text>
        <text class="balance-value">{{ availableBalance }}</text>
      </view>

      <view v-if="!bankAccount" class="form-section bind-section">
        <view class="section-head">
          <view>
            <text class="section-title">Bind bank account</text>
            <text class="row-meta">One bank account can be bound to each user.</text>
          </view>
          <text class="step-label">Step 1</text>
        </view>
        <view class="form-grid">
          <view class="form-field">
            <text class="field-label">Country or region</text>
            <input v-model="form.country" class="field-input" placeholder="Nigeria, India or Ghana" />
          </view>
          <view class="form-field">
            <text class="field-label">Account holder</text>
            <input v-model="form.accountName" class="field-input" placeholder="Full account holder name" />
          </view>
          <view class="form-field">
            <text class="field-label">Bank name</text>
            <input v-model="form.bankName" class="field-input" placeholder="Enter bank name" />
          </view>
          <view class="form-field">
            <text class="field-label">Account number</text>
            <input v-model="form.accountNumber" class="field-input" type="number" placeholder="Enter bank account number" />
          </view>
        </view>
        <button class="primary-button submit-button" @click="bindAccount">Bind bank account</button>
      </view>

      <template v-else>
        <view class="bank-section bound-section">
          <view class="section-head">
            <view>
              <text class="section-title">Bank account</text>
              <text class="row-meta">Used for this withdrawal</text>
            </view>
            <text class="status-pill active">Bound</text>
          </view>
          <view class="bound-bank">
            <text class="row-title">{{ bankAccount.bankName }}</text>
            <text class="row-meta">{{ bankAccount.accountName }} / {{ bankAccount.maskedAccountNumber }}</text>
            <text class="row-meta">{{ bankAccount.country }} / Bound {{ bankAccount.createdAt }}</text>
          </view>
        </view>

        <view class="form-section request-section">
          <view class="section-head">
            <view>
              <text class="section-title">{{ lotteryRecordId ? 'Prize withdrawal' : 'Withdrawal request' }}</text>
              <text class="row-meta">Confirm the amount and your contact details.</text>
            </view>
            <text class="step-label">Final step</text>
          </view>
          <view v-if="!lotteryRecordId" class="form-field">
            <text class="field-label">Amount</text>
            <input v-model="form.amount" class="field-input" type="number" placeholder="Enter withdrawal amount" />
          </view>
          <view class="form-field contact-field">
            <text class="field-label">Contact</text>
            <input v-model="form.contact" class="field-input" placeholder="Phone number or WhatsApp" />
          </view>
          <button class="primary-button submit-button" @click="submit">
            {{ lotteryRecordId ? 'Submit prize withdrawal' : 'Submit withdrawal' }}
          </button>
        </view>
      </template>

      <view v-if="store.state.withdrawals.length" class="history-section">
        <view class="section-head">
          <text class="section-title">Withdrawal history</text>
          <text class="history-count">{{ store.state.withdrawals.length }}</text>
        </view>
        <view v-for="item in store.state.withdrawals" :key="item.id" class="withdraw-row">
          <view class="row-copy">
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
      notice.value = 'Complete all bank account fields.'
      return
    }
    bankAccount.value = await bindBankAccount({
      country: form.country.trim(),
      accountName: form.accountName.trim(),
      bankName: form.bankName.trim(),
      accountNumber: form.accountNumber.trim()
    })
    notice.value = lotteryRecordId.value
      ? 'Bank account bound. You can now submit the prize withdrawal.'
      : 'Bank account bound.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to bind bank account'
  }
}

async function submit() {
  try {
    if (!bankAccount.value) {
      notice.value = 'Bind a bank account first.'
      return
    }
    if (!lotteryRecordId.value && !form.amount.trim()) {
      notice.value = 'Enter a withdrawal amount.'
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
          sendChatMessage: false
        })
    uni.setStorageSync('pending-support-draft', buildWithdrawalDraft(withdrawal.requestNo))
    await store.bootstrap().catch(() => undefined)
    uni.redirectTo({ url: '/pages/support/index' })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Withdrawal request failed'
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
.withdraw-page {
  padding-bottom: 48rpx;
}

.page-header-copy .eyebrow,
.page-header-copy .title,
.page-header-copy .subtitle {
  display: block;
}

.page-header-copy .title {
  margin-top: 7rpx;
}

.page-header-copy .subtitle {
  margin-top: 8rpx;
}

.withdraw-hero {
  padding: 28rpx;
  align-items: center;
  border-bottom: 0;
}

.balance-panel,
.form-section,
.bank-section,
.history-section {
  background: #ffffff;
  border: 1rpx solid var(--cb-line);
  border-radius: 12rpx;
  overflow: hidden;
}

.balance-panel {
  padding: 28rpx;
  border-left: 5rpx solid var(--cb-mint-strong);
}

.balance-label,
.balance-value,
.row-title,
.row-meta {
  display: block;
}

.balance-label {
  color: #6f7178;
  font-size: 22rpx;
}

.balance-value {
  margin-top: 10rpx;
  color: #0f7f49;
  font-size: 44rpx;
  font-weight: 700;
}

.section-head {
  min-height: 82rpx;
  padding: 16rpx 24rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  border-bottom: 1rpx solid #dedfe3;
}

.section-head .row-meta {
  margin-top: 4rpx;
}

.step-label,
.history-count {
  flex: 0 0 auto;
  color: #002fa7;
  font-size: 21rpx;
  font-weight: 700;
  text-transform: uppercase;
}

.form-grid {
  padding: 24rpx 24rpx 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 20rpx;
}

.form-section > .form-field {
  padding: 24rpx 24rpx 0;
}

.contact-field {
  padding-top: 20rpx !important;
}

.submit-button {
  width: calc(100% - 48rpx);
  margin: 26rpx 24rpx 24rpx;
  background: var(--cb-mint-strong);
}

.bind-section {
  background: var(--cb-sky);
  border-color: #cfe4fb;
}

.request-section,
.bound-section {
  background: var(--cb-mint);
  border-color: #c6ead8;
}

.bound-bank {
  padding: 22rpx 24rpx;
  border-left: 4rpx solid var(--cb-mint-strong);
}

.row-title {
  color: #111111;
  font-size: 26rpx;
  font-weight: 700;
}

.row-meta {
  margin-top: 6rpx;
  color: #777980;
  font-size: 21rpx;
  line-height: 1.4;
}

.withdraw-row {
  min-height: 98rpx;
  padding: 18rpx 24rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  border-bottom: 1rpx solid #dedfe3;
}

.withdraw-row:last-child {
  border-bottom: 0;
}

.row-copy {
  min-width: 0;
}

.amount-side {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8rpx;
}

.amount {
  color: #002fa7;
  font-size: 26rpx;
  font-weight: 700;
}

.status-pill.warning {
  color: #9a5b00;
  background: #fff4df;
}

.notice-text {
  display: block;
  color: #b42318;
  font-size: 23rpx;
  text-align: center;
}

@media (min-width: 768px) {
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
