<template>
  <view class="page-shell soft-page">
    <view v-if="isAdminReady" class="page-stack">
      <view class="admin-top-nav">
        <button class="nav-button" @click="goAdminConsole">管理员总控台</button>
        <button class="nav-button" @click="goSupportChat">客服聊天</button>
        <button class="nav-button active" @click="store.refreshRates">汇率管理</button>
        <button class="nav-button" @click="goUserHome">用户端首页</button>
      </view>

      <view class="panel">
        <text class="eyebrow">汇率管理</text>
        <view style="height: 12rpx"></view>
        <text class="title">维护礼品卡实时汇率</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">管理员可以新增、编辑、暂停或删除礼品卡汇率记录。</text>
      </view>

      <view class="panel">
        <text class="field-label">卡种名称</text>
        <input v-model="form.cardName" class="field-input" placeholder="Sephora, Apple(itunes), Steam..." />
        <view style="height: 20rpx"></view>

        <text class="field-label">地区</text>
        <picker
          mode="selector"
          :range="store.state.countries"
          range-key="name"
          :value="selectedRegionIndex"
          @change="handleRegionChange"
        >
          <view class="field-input select-field">
            <text>{{ selectedRegionLabel }}</text>
            <text class="select-code">{{ form.region }}</text>
          </view>
        </picker>
        <view style="height: 20rpx"></view>

        <text class="field-label">汇率</text>
        <input v-model="form.rate" class="field-input" placeholder="NGN 99900 / $100" />
        <view style="height: 24rpx"></view>

        <button class="primary-button" @click="submitRate">{{ editingId ? '更新汇率' : '创建汇率' }}</button>
        <view style="height: 16rpx"></view>
        <text v-if="notice" class="muted">{{ notice }}</text>
      </view>

      <view class="panel">
        <text class="section-title">汇率列表</text>
        <view style="height: 20rpx"></view>
        <view v-for="rate in store.state.rates" :key="rate.id" class="rate-row">
          <view>
            <text class="rate-name">{{ rate.cardName }}</text>
            <text class="rate-meta">{{ rate.region }} · {{ rate.updatedAt }}</text>
          </view>
          <view class="rate-side">
            <text class="rate-value">{{ rate.rate }}</text>
            <view class="rate-actions">
              <button class="ghost-button toggle-button" @click="startEdit(rate)">编辑</button>
              <button
                class="ghost-button toggle-button"
                @click="toggleRate(rate.id, rate.status === 'active' ? 'paused' : 'active')"
              >
                {{ rate.status === 'active' ? '暂停' : '启用' }}
              </button>
              <button class="ghost-button toggle-button danger-action" @click="deleteRate(rate.id)">删除</button>
            </view>
          </view>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import { useAppStore } from '@/store/app'
import type { RateItem } from '@/types'

const store = useAppStore()
const isAdminReady = ref(false)
const notice = ref('')
const editingId = ref('')

const form = reactive({
  cardName: '',
  region: defaultRegionCode(),
  rate: ''
})

const selectedRegionIndex = computed(() => {
  const index = store.state.countries.findIndex((country) => country.code === form.region)
  return index >= 0 ? index : 0
})

const selectedRegionLabel = computed(() => {
  return store.state.countries[selectedRegionIndex.value]?.name || '请选择地区'
})

async function submitRate() {
  try {
    if (editingId.value) {
      await store.updateRate(editingId.value, {
        cardName: form.cardName,
        region: form.region || defaultRegionCode(),
        rate: form.rate
      })
      notice.value = '汇率已更新。'
    } else {
      await store.addRate({
        cardName: form.cardName,
        region: form.region || defaultRegionCode(),
        rate: form.rate
      })
      notice.value = '汇率已创建。'
    }
    form.cardName = ''
    form.region = defaultRegionCode()
    form.rate = ''
    editingId.value = ''
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '提交失败'
  }
}

function startEdit(rate: RateItem) {
  editingId.value = rate.id
  form.cardName = rate.cardName
  form.region = resolveRegionCode(rate.region)
  form.rate = rate.rate
  notice.value = `正在编辑 ${rate.cardName}。`
}

function handleRegionChange(event: { detail: { value: number | string } }) {
  const index = Number(event.detail.value)
  const country = store.state.countries[index]
  if (country) {
    form.region = country.code
  }
}

function defaultRegionCode() {
  return store.state.countries[0]?.code || 'NG'
}

function resolveRegionCode(region: string) {
  const normalized = region.trim().replace(/^[+＋]/, '').replace(/[\s_-]+/g, '').toUpperCase()
  const aliases: Record<string, string> = {
    NG: 'NG',
    NIGERIA: 'NG',
    '234': 'NG',
    尼日利亚: 'NG',
    IN: 'IN',
    INDIA: 'IN',
    '91': 'IN',
    印度: 'IN',
    CM: 'CM',
    CAMEROON: 'CM',
    '237': 'CM',
    喀麦隆: 'CM',
    GH: 'GH',
    GHANA: 'GH',
    '233': 'GH',
    加纳: 'GH'
  }
  return aliases[normalized] || defaultRegionCode()
}

async function toggleRate(rateId: string, status: RateItem['status']) {
  try {
    await store.updateRateStatus(rateId, status)
    notice.value = status === 'active' ? '汇率已启用。' : '汇率已暂停。'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '状态更新失败'
  }
}

async function deleteRate(rateId: string) {
  try {
    await store.deleteRate(rateId)
    notice.value = '汇率已删除。'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '删除失败'
  }
}

function requireAdmin() {
  if (store.state.currentUser?.roleCode === 'ADMIN') {
    isAdminReady.value = true
    return true
  }
  isAdminReady.value = false
  notice.value = '需要管理员账号。'
  uni.redirectTo({ url: '/pages/admin-login/index' })
  return false
}

function goAdminConsole() {
  uni.redirectTo({ url: '/pages/admin-console/index' })
}

function goSupportChat() {
  uni.redirectTo({ url: '/pages/support-chat-v2/index' })
}

function goUserHome() {
  uni.redirectTo({ url: '/pages/home/index' })
}

onShow(() => {
  if (requireAdmin()) {
    store.refreshRates().catch((error) => {
      notice.value = error instanceof Error ? error.message : '汇率加载失败'
    })
  }
})
</script>

<style scoped lang="scss">
.admin-top-nav {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12rpx;
}

.nav-button {
  margin: 0;
  padding: 18rpx 12rpx;
  border: 1rpx solid #d9dde3;
  border-radius: 8rpx;
  background: #ffffff;
  color: #101820;
  box-shadow: none;
  font-size: 24rpx;
  font-weight: 800;
  line-height: 1.2;
}

.nav-button.active {
  color: #ffffff;
  border-color: #002fa7;
  background: #002fa7;
}

.select-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.select-code {
  color: #0088cc;
  font-size: 24rpx;
  font-weight: 800;
}

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

@media (max-width: 760px) {
  .admin-top-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
