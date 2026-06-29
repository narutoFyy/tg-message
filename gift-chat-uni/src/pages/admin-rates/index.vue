<template>
  <view class="page-shell soft-page">
    <view v-if="isAdminReady" class="page-stack">
      <view class="panel">
        <text class="eyebrow">Admin Rates</text>
        <view style="height: 12rpx"></view>
        <text class="title">Maintain live gift card rates</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">Administrator accounts can publish, edit, pause, and remove rate records.</text>
      </view>

      <view class="panel">
        <text class="field-label">Card Name</text>
        <input v-model="form.cardName" class="field-input" placeholder="Sephora, Apple(itunes), Steam..." />
        <view style="height: 20rpx"></view>

        <text class="field-label">Region</text>
        <input v-model="form.region" class="field-input" placeholder="NG / US / UK / EU" />
        <view style="height: 20rpx"></view>

        <text class="field-label">Rate</text>
        <input v-model="form.rate" class="field-input" placeholder="NGN 99900 / $100" />
        <view style="height: 24rpx"></view>

        <button class="primary-button" @click="submitRate">{{ editingId ? 'Update Rate' : 'Create Rate' }}</button>
        <view style="height: 16rpx"></view>
        <text v-if="notice" class="muted">{{ notice }}</text>
      </view>

      <view class="panel">
        <text class="section-title">Rate feed</text>
        <view style="height: 20rpx"></view>
        <view v-for="rate in store.state.rates" :key="rate.id" class="rate-row">
          <view>
            <text class="rate-name">{{ rate.cardName }}</text>
            <text class="rate-meta">{{ rate.region }} · {{ rate.updatedAt }}</text>
          </view>
          <view class="rate-side">
            <text class="rate-value">{{ rate.rate }}</text>
            <view class="rate-actions">
              <button class="ghost-button toggle-button" @click="startEdit(rate)">Edit</button>
              <button
                class="ghost-button toggle-button"
                @click="toggleRate(rate.id, rate.status === 'active' ? 'paused' : 'active')"
              >
                {{ rate.status === 'active' ? 'Pause' : 'Activate' }}
              </button>
              <button class="ghost-button toggle-button danger-action" @click="deleteRate(rate.id)">Delete</button>
            </view>
          </view>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import { useAppStore } from '@/store/app'
import type { RateItem } from '@/types'

const store = useAppStore()
const isAdminReady = ref(false)
const notice = ref('')
const editingId = ref('')

const form = reactive({
  cardName: '',
  region: '',
  rate: ''
})

async function submitRate() {
  try {
    if (editingId.value) {
      await store.updateRate(editingId.value, {
        cardName: form.cardName,
        region: form.region,
        rate: form.rate
      })
      notice.value = 'Rate updated.'
    } else {
      await store.addRate({
        cardName: form.cardName,
        region: form.region,
        rate: form.rate
      })
      notice.value = 'Rate created.'
    }
    form.cardName = ''
    form.region = ''
    form.rate = ''
    editingId.value = ''
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Submit failed'
  }
}

function startEdit(rate: RateItem) {
  editingId.value = rate.id
  form.cardName = rate.cardName
  form.region = rate.region
  form.rate = rate.rate
  notice.value = `Editing ${rate.cardName}.`
}

async function toggleRate(rateId: string, status: RateItem['status']) {
  try {
    await store.updateRateStatus(rateId, status)
    notice.value = `Rate moved to ${status}.`
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Status update failed'
  }
}

async function deleteRate(rateId: string) {
  try {
    await store.deleteRate(rateId)
    notice.value = 'Rate deleted.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Delete failed'
  }
}

function requireAdmin() {
  if (store.state.currentUser?.roleCode === 'ADMIN') {
    isAdminReady.value = true
    return true
  }
  isAdminReady.value = false
  notice.value = 'Admin account required.'
  uni.redirectTo({ url: '/pages/admin-login/index' })
  return false
}

onShow(() => {
  if (requireAdmin()) {
    store.refreshRates().catch((error) => {
      notice.value = error instanceof Error ? error.message : 'Rates failed'
    })
  }
})
</script>

<style scoped lang="scss">
.rate-row {
  padding: 18rpx 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18rpx;
  border-bottom: 1rpx solid #eef1f3;
}

.rate-name {
  display: block;
  font-size: 28rpx;
  font-weight: 800;
}

.rate-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #8d8d8d;
}

.rate-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12rpx;
}

.rate-actions {
  display: flex;
  gap: 10rpx;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.rate-value {
  font-size: 26rpx;
  font-weight: 800;
  color: #181818;
  text-align: right;
}

.toggle-button {
  min-width: 156rpx;
  padding-top: 14rpx;
  padding-bottom: 14rpx;
  font-size: 22rpx;
}

.danger-action {
  color: #d65a4e;
  border-color: #f0b1ab;
}
</style>
