<template>
  <view class="page-shell soft-page loan-page">
    <view class="page-stack">
      <view class="page-header loan-hero tone-reward">
        <view class="page-header-copy"><text class="eyebrow">Finance</text><text class="title">Loan application</text><text class="subtitle">Submit an amount and repayment plan for review.</text></view>
      </view>
      <view class="form-section loan-form">
        <view class="section-head"><text class="section-title">New application</text><text class="step-label">Required fields</text></view>
        <view class="form-grid">
          <view class="form-field"><text class="field-label">Amount</text><input v-model="form.amount" class="field-input" placeholder="e.g. NGN 100,000" /></view>
          <view class="form-field"><text class="field-label">Country</text><input v-model="form.country" class="field-input" placeholder="Nigeria, India, Cameroon or Ghana" /></view>
          <view class="form-field form-wide"><text class="field-label">Purpose</text><input v-model="form.purpose" class="field-input" placeholder="Why do you need this loan?" /></view>
          <view class="form-field"><text class="field-label">Contact</text><input v-model="form.contact" class="field-input" placeholder="Phone or WhatsApp" /></view>
          <view class="form-field"><text class="field-label">Repayment plan</text><input v-model="form.repaymentPlan" class="field-input" placeholder="e.g. repay after next payout" /></view>
        </view>
        <button class="primary-button submit-button" :disabled="submitting" @click="submitLoan">{{ submitting ? 'Submitting...' : 'Submit application' }}</button>
        <text v-if="notice" class="notice-text">{{ notice }}</text>
      </view>
      <view class="records-section">
        <view class="section-head"><text class="section-title">Loan records</text><text class="record-count">{{ store.state.loans.length }}</text></view>
        <view v-for="loan in store.state.loans" :key="loan.id" class="loan-row">
          <view class="row-copy"><text class="row-title">{{ loan.applicationNo }}</text><text class="row-meta">{{ loan.amount }} / {{ loan.country }} / {{ loan.createdAt }}</text><text class="row-meta">{{ loan.purpose }}</text><text v-if="loan.reviewNote" class="row-note">{{ loan.reviewNote }}</text></view>
          <text :class="['status-pill', statusClass(loan.status)]">{{ loan.status }}</text>
        </view>
        <view v-if="!store.state.loans.length" class="empty-row"><text class="muted">No loan applications yet.</text></view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import type { LoanApplicationItem } from '@/types'
const store = useAppStore(); const notice = ref(''); const submitting = ref(false)
const form = reactive({ amount: '', country: '', purpose: '', contact: '', repaymentPlan: '' })
onShow(() => { store.bootstrap() })
async function submitLoan() {
  if (!form.amount.trim() || !form.country.trim() || !form.purpose.trim()) { notice.value = 'Amount, country, and purpose are required.'; return }
  submitting.value = true
  try {
    const loan = await store.createLoanApplication({ amount: form.amount, country: form.country, purpose: form.purpose, contact: form.contact || undefined, repaymentPlan: form.repaymentPlan || undefined, sendChatMessage: false })
    uni.setStorageSync('pending-support-draft', buildLoanDraft(loan.applicationNo)); notice.value = 'Application submitted. Review and send it in support chat.'
    form.amount = ''; form.country = ''; form.purpose = ''; form.contact = ''; form.repaymentPlan = ''
    setTimeout(() => uni.redirectTo({ url: '/pages/support/index' }), 450)
  } catch (error) { notice.value = error instanceof Error ? error.message : 'Submit failed' } finally { submitting.value = false }
}
function buildLoanDraft(applicationNo: string) { return [`Loan application ${applicationNo}`, `Amount: ${form.amount}`, `Country: ${form.country}`, `Purpose: ${form.purpose}`, form.contact ? `Contact: ${form.contact}` : '', form.repaymentPlan ? `Repayment plan: ${form.repaymentPlan}` : ''].filter(Boolean).join('\n') }
function statusClass(status: LoanApplicationItem['status']) { return { pending: 'warning', approved: 'active', rejected: 'danger' }[status] }
</script>

<style scoped lang="scss">
.loan-page { padding-bottom: 48rpx; }
.page-header-copy .eyebrow, .page-header-copy .title, .page-header-copy .subtitle { display: block; }
.page-header-copy .title { margin-top: 7rpx; }
.page-header-copy .subtitle { margin-top: 8rpx; }
.loan-hero { padding: 28rpx; align-items: center; border-bottom: 0; }
.form-section, .records-section { background: #ffffff; border: 1rpx solid var(--cb-line); border-radius: 12rpx; overflow: hidden; }
.loan-form { background: var(--cb-amber); border-color: #f0daa4; }
.section-head { min-height: 76rpx; padding: 0 24rpx; display: flex; align-items: center; justify-content: space-between; gap: 20rpx; border-bottom: 1rpx solid #dedfe3; }
.step-label, .record-count { color: #002fa7; font-size: 21rpx; font-weight: 700; }
.form-grid { padding: 24rpx 24rpx 0; display: grid; grid-template-columns: minmax(0, 1fr); gap: 20rpx; }
.submit-button { width: calc(100% - 48rpx); margin: 26rpx 24rpx 24rpx; background: var(--cb-amber-strong); color: #382600; }
.notice-text { display: block; padding: 0 24rpx 24rpx; color: #6f7178; font-size: 23rpx; text-align: center; }
.loan-row { min-height: 102rpx; padding: 18rpx 24rpx; box-sizing: border-box; display: flex; align-items: flex-start; justify-content: space-between; gap: 18rpx; border-bottom: 1rpx solid #dedfe3; }
.loan-row:last-child { border-bottom: 0; }
.row-copy { min-width: 0; }
.row-title { display: block; color: #111111; font-size: 26rpx; font-weight: 700; }
.row-meta, .row-note { display: block; margin-top: 6rpx; color: #6f7178; font-size: 21rpx; line-height: 1.4; }
.row-note { color: #137a4e; }
.status-pill.warning { color: #9a5b00; background: #fff4df; }
.status-pill.danger { color: #b42318; background: #fff0ee; }
.empty-row { padding: 30rpx 24rpx; }
@media (min-width: 768px) { .form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .form-wide { grid-column: 1 / -1; } }
</style>
