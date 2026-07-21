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
        <view class="info-row birthday-row">
          <view><text class="info-label">Birthday</text><text v-if="birthdayLocked" class="locked-note">Locked after first save</text></view>
          <text v-if="birthdayLocked" class="info-value">{{ benefits?.birthDate }}</text>
          <picker v-else mode="date" :value="birthdayDraft" :end="today" @change="saveBirthday">
            <view class="birthday-picker">{{ birthdayDraft || 'Set birthday' }}</view>
          </picker>
        </view>
        <text v-if="birthdayLocked" class="birthday-help">Contact customer support if the saved birthday needs correction.</text>
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
          <view v-if="changingBank" class="bank-change-form">
            <input v-model="replaceForm.country" class="bank-input" placeholder="Country or region" />
            <input v-model="replaceForm.accountName" class="bank-input" placeholder="Account holder" />
            <input v-model="replaceForm.bankName" class="bank-input" placeholder="Bank name" />
            <input v-model="replaceForm.accountNumber" class="bank-input" type="number" placeholder="New account number" />
            <view class="bank-actions"><button class="ghost-button" @click="changingBank = false">Cancel</button><button class="primary-button" @click="saveBank">Save bank account</button></view>
          </view>
          <button v-else class="ghost-button change-bank-button" @click="startBankChange">Change bank account</button>
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
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { fetchMyBankAccount, replaceBankAccount } from '@/utils/api'
import type { BankAccountItem } from '@/types'

const store = useAppStore()
const bankAccount = ref<BankAccountItem | null>(null)
const notice = ref('')
const changingBank = ref(false)
const replaceForm = reactive({ country: '', accountName: '', bankName: '', accountNumber: '' })

onShow(() => {
  store.bootstrap()
  store.refreshCurrentAccount().catch(() => undefined)
  store.refreshVipBenefits().catch(() => undefined)
  refreshBankAccount()
})

const user = computed(() => store.state.currentUser)
const roleLabel = computed(() => user.value?.roleCode || 'USER')
const benefits = computed(() => store.state.vipBenefits)
const birthdayLocked = computed(() => Boolean(benefits.value?.birthdayLocked && benefits.value.birthDate))
const birthdayDraft = ref('')
const today = new Date().toISOString().slice(0, 10)
const safeBankNumber = computed(() => bankAccount.value?.maskedAccountNumber || '****')

async function saveBirthday(event: { detail: { value: string } }) {
  const birthDate = event.detail.value
  if (!birthDate) return
  try {
    await store.setVipBirthday(birthDate)
    birthdayDraft.value = birthDate
    notice.value = 'Birthday saved. Contact support if it ever needs correction.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Birthday update failed.'
  }
}

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
function startBankChange() {
  if (!bankAccount.value) return
  replaceForm.country = bankAccount.value.country
  replaceForm.accountName = bankAccount.value.accountName
  replaceForm.bankName = bankAccount.value.bankName
  replaceForm.accountNumber = ''
  notice.value = ''
  changingBank.value = true
}
async function saveBank() {
  if (!replaceForm.country.trim() || !replaceForm.accountName.trim() || !replaceForm.bankName.trim() || !replaceForm.accountNumber.trim()) {
    notice.value = 'Complete all bank account fields.'
    return
  }
  try {
    bankAccount.value = await replaceBankAccount({
      country: replaceForm.country.trim(), accountName: replaceForm.accountName.trim(), bankName: replaceForm.bankName.trim(), accountNumber: replaceForm.accountNumber.trim()
    })
    changingBank.value = false
    notice.value = 'Bank account updated.'
  } catch (error) { notice.value = error instanceof Error ? error.message : 'Bank account update failed.' }
}
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
.birthday-row { border-bottom: 0; }
.locked-note { display: block; margin-top: 4rpx; color: #6f7178; font-size: 19rpx; }
.birthday-picker { min-width: 190rpx; padding: 12rpx 16rpx; border: 1rpx solid #b8c9ef; border-radius: 4rpx; background: #ffffff; color: #002fa7; font-size: 23rpx; font-weight: 700; text-align: center; }
.birthday-help { display: block; padding: 0 24rpx 18rpx; color: #6f7178; font-size: 20rpx; line-height: 1.4; }
.bank-summary { padding: 24rpx; border-bottom: 1rpx solid #c6ead8; border-left: 5rpx solid var(--cb-mint-strong); background: rgba(255, 255, 255, 0.72); }
.bank-name, .bank-number { display: block; }
.bank-name { color: #111111; font-size: 25rpx; font-weight: 700; }
.bank-number { margin-top: 12rpx; color: #0f7f49; font-size: 32rpx; font-weight: 700; }
.empty-bank { padding: 30rpx 24rpx 24rpx; }
.empty-title { display: block; color: #111111; font-size: 27rpx; font-weight: 700; }
.empty-copy { display: block; margin-top: 9rpx; color: #6f7178; font-size: 24rpx; line-height: 1.4; }
.bind-button { width: 100%; margin-top: 24rpx; }
.change-bank-button { width: calc(100% - 48rpx); margin: 20rpx 24rpx 24rpx; }
.bank-change-form { padding: 20rpx 24rpx 24rpx; }
.bank-input { width: 100%; height: 76rpx; margin-top: 12rpx; padding: 0 18rpx; box-sizing: border-box; background: #ffffff; border: 1rpx solid #c8c9cf; border-radius: 4rpx; font-size: 24rpx; }
.bank-actions { margin-top: 18rpx; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12rpx; }
.bank-actions button { width: 100%; margin: 0; }
.notice-text { display: block; color: #6f7178; font-size: 23rpx; text-align: center; }
</style>
