<template>
  <view class="page-shell soft-page loan-page">
    <view class="panel hero-panel">
      <text class="eyebrow">Loan</text>
      <view style="height: 12rpx"></view>
      <text class="title">Apply for a loan</text>
      <view style="height: 10rpx"></view>
      <text class="subtitle">Submit the amount and repayment plan. Admin will review it and your support agent will see the request.</text>
    </view>

    <view class="panel">
      <text class="field-label">Amount</text>
      <input v-model="form.amount" class="field-input" placeholder="e.g. ₦100000" />
      <view style="height: 18rpx"></view>

      <text class="field-label">Country</text>
      <input v-model="form.country" class="field-input" placeholder="Nigeria / India / Cameroon / Ghana" />
      <view style="height: 18rpx"></view>

      <text class="field-label">Purpose</text>
      <input v-model="form.purpose" class="field-input" placeholder="Why do you need this loan?" />
      <view style="height: 18rpx"></view>

      <text class="field-label">Contact</text>
      <input v-model="form.contact" class="field-input" placeholder="Phone / WhatsApp" />
      <view style="height: 18rpx"></view>

      <text class="field-label">Repayment Plan</text>
      <input v-model="form.repaymentPlan" class="field-input" placeholder="e.g. repay after next payout" />
      <view style="height: 24rpx"></view>

      <button class="primary-button" :disabled="submitting" @click="submitLoan">
        {{ submitting ? 'Submitting...' : 'Submit Application' }}
      </button>
      <view style="height: 14rpx"></view>
      <text v-if="notice" class="muted">{{ notice }}</text>
    </view>

    <view class="panel">
      <text class="section-title">Loan records</text>
      <view style="height: 18rpx"></view>
      <view v-for="loan in store.state.loans" :key="loan.id" class="loan-row">
        <view>
          <text class="row-title">{{ loan.applicationNo }}</text>
          <text class="row-meta">{{ loan.amount }} / {{ loan.country }} / {{ loan.createdAt }}</text>
          <text class="row-meta">{{ loan.purpose }}</text>
          <text v-if="loan.reviewNote" class="row-note">{{ loan.reviewNote }}</text>
        </view>
        <text :class="['status-pill', statusClass(loan.status)]">{{ loan.status }}</text>
      </view>
      <text v-if="!store.state.loans.length" class="muted">No loan applications yet.</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import type { LoanApplicationItem } from '@/types'

const store = useAppStore()
const notice = ref('')
const submitting = ref(false)
const form = reactive({
  amount: '',
  country: '',
  purpose: '',
  contact: '',
  repaymentPlan: ''
})

onShow(() => {
  store.bootstrap()
})

async function submitLoan() {
  if (!form.amount.trim() || !form.country.trim() || !form.purpose.trim()) {
    notice.value = 'Amount, country, and purpose are required.'
    return
  }

  submitting.value = true
  try {
    await store.createLoanApplication({
      amount: form.amount,
      country: form.country,
      purpose: form.purpose,
      contact: form.contact || undefined,
      repaymentPlan: form.repaymentPlan || undefined
    })
    notice.value = 'Application submitted. Your support chat has been notified.'
    form.amount = ''
    form.country = ''
    form.purpose = ''
    form.contact = ''
    form.repaymentPlan = ''
    setTimeout(() => uni.redirectTo({ url: '/pages/support/index' }), 450)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Submit failed'
  } finally {
    submitting.value = false
  }
}

function statusClass(status: LoanApplicationItem['status']) {
  return {
    pending: 'warning',
    approved: 'active',
    rejected: 'danger'
  }[status]
}
</script>

<style scoped lang="scss">
.loan-page {
  padding-bottom: 80rpx;
}

.hero-panel {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.96)),
    linear-gradient(135deg, #dff9ff, #e9fff1);
}

.loan-row {
  padding: 18rpx 0;
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  border-bottom: 1rpx solid #eef1f3;
}

.row-title {
  display: block;
  font-size: 28rpx;
  font-weight: 900;
  color: #171717;
}

.row-meta,
.row-note {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #737d87;
  line-height: 1.4;
}

.row-note {
  color: #0e9f58;
}

.status-pill.warning {
  color: #dd8a25;
  background: rgba(255, 184, 76, 0.2);
}

.status-pill.danger {
  color: #d64242;
  background: rgba(244, 91, 91, 0.14);
}
</style>
