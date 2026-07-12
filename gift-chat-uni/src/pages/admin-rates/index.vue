<template>
  <view class="page-shell admin-rate-page">
    <view v-if="isAdminReady" class="page-stack">
      <view class="admin-top-nav">
        <button class="nav-button" @click="goAdminConsole">管理员总控台</button>
        <button class="nav-button" @click="goSupportChat">客服聊天</button>
        <button class="nav-button active" @click="store.refreshRates">汇率管理</button>
        <button class="nav-button" @click="goUserHome">用户端首页</button>
      </view>

      <view class="page-heading">
        <view>
          <text class="eyebrow">汇率管理</text>
          <text class="title">维护礼品卡实时汇率</text>
          <text class="subtitle">从预设品牌中选择卡片，或手动添加其他卡片。</text>
        </view>
        <view class="heading-count">
          <text class="count-value">{{ store.state.rates.length }}</text>
          <text class="count-label">条汇率</text>
        </view>
      </view>

      <view class="general-rate-panel">
        <view class="panel-heading list-heading">
          <view>
            <text class="section-title">通用货币汇率（钱包 / 转盘）</text>
            <text class="panel-note">美元是统一参考单位；这里维护 1 USD 可兑换的当地币数量。</text>
          </view>
          <button class="text-button" @click="loadGeneralRates">刷新</button>
        </view>
        <view class="general-rate-grid">
          <view v-for="rate in generalRates" :key="rate.countryCode" class="general-rate-row">
            <view>
              <text class="rate-name">{{ rate.countryName }} · {{ rate.currencyCode }}</text>
              <text class="rate-meta">{{ rate.displayRate }} · {{ rate.enabled ? '已启用' : '已停用' }}</text>
            </view>
            <input v-model.trim="generalRateInputs[rate.countryCode]" class="field-input general-rate-input" type="number" :disabled="rate.countryCode === 'US'" />
            <button class="ghost-button action-button" @click="saveGeneralRate(rate)">保存</button>
          </view>
        </view>
      </view>

      <view class="rate-workspace">
        <view class="editor-panel">
          <view class="panel-heading">
            <view>
              <text class="section-title">{{ editingId ? '编辑汇率' : '新增汇率' }}</text>
              <text class="panel-note">预设卡片会自动绑定标准名称和品牌图标。</text>
            </view>
            <button v-if="editingId" class="text-button" @click="resetForm()">取消编辑</button>
          </view>

          <text class="field-label">卡片录入方式</text>
          <view class="mode-switch">
            <button :class="['mode-button', cardMode === 'preset' && 'active']" @click="setCardMode('preset')">预设卡片</button>
            <button :class="['mode-button', cardMode === 'manual' && 'active']" @click="setCardMode('manual')">手动输入</button>
          </view>

          <template v-if="cardMode === 'preset'">
            <text class="field-label field-gap">选择卡片</text>
            <button class="card-select-trigger" @click="openCardSelector">
              <image class="selected-card-logo" :src="selectedCardLogo" mode="aspectFit" />
              <view class="selected-card-copy">
                <text :class="['selected-card-name', !selectedCard && 'placeholder']">{{ selectedCard?.name || '请选择预设卡片' }}</text>
                <text class="selected-card-code">{{ selectedCard?.code || '11 个常用品牌' }}</text>
              </view>
              <image class="select-chevron" :src="uiIcons.chevronDown" mode="aspectFit" />
            </button>
          </template>

          <template v-else>
            <text class="field-label field-gap">卡种名称</text>
            <input v-model.trim="form.cardName" class="field-input" maxlength="128" placeholder="输入其他礼品卡名称" />
            <view class="custom-preview">
              <image class="custom-preview-logo" :src="cardLogoFor(null, form.cardName)" mode="aspectFit" />
              <view>
                <text class="preview-title">{{ matchedManualCard?.name || form.cardName || '自定义卡片' }}</text>
                <text class="preview-note">{{ matchedManualCard ? '提交后将自动匹配预设品牌' : '无法匹配时使用默认卡片图标' }}</text>
              </view>
            </view>
          </template>

          <text class="field-label field-gap">地区</text>
          <picker mode="selector" :range="store.state.countries" range-key="name" :value="selectedRegionIndex" @change="handleRegionChange">
            <view class="field-input select-field">
              <text>{{ selectedRegionLabel }}</text>
              <text class="select-code">{{ form.region }}</text>
            </view>
          </picker>

          <text class="field-label field-gap">礼品卡回收汇率（当地币 / 1 USD）</text>
          <input v-model.trim="form.rate" class="field-input" type="number" placeholder="例如：999" />

          <button class="primary-button submit-button" @click="submitRate">{{ editingId ? '更新汇率' : '创建汇率' }}</button>
          <text v-if="notice" class="form-notice">{{ notice }}</text>
        </view>

        <view class="rates-panel">
          <view class="panel-heading list-heading">
            <view>
              <text class="section-title">汇率记录</text>
              <text class="panel-note">卡片图标和标准名称将在用户端同步显示。</text>
            </view>
            <button class="text-button" @click="store.refreshRates">刷新</button>
          </view>

          <view v-if="!store.state.rates.length" class="empty-state">
            <text class="section-title">暂无汇率记录</text>
            <text class="panel-note">通过左侧表单创建第一条汇率。</text>
          </view>

          <view v-for="rate in store.state.rates" :key="rate.id" class="rate-row">
            <view class="rate-identity">
              <image class="rate-logo" :src="cardLogoFor(rate.cardCode, rate.cardName)" mode="aspectFit" />
              <view class="rate-copy">
                <view class="rate-name-line">
                  <text class="rate-name">{{ rate.cardName }}</text>
                  <text v-if="!rate.cardCode" class="custom-badge">自定义</text>
                  <text :class="['status-badge', rate.status]">{{ rate.status === 'active' ? '已启用' : '已暂停' }}</text>
                </view>
                <text class="rate-meta">{{ rate.region }} · 更新于 {{ rate.updatedAt }}</text>
              </view>
            </view>
            <view class="rate-side">
              <text class="rate-value">{{ rate.rate }}</text>
              <view class="rate-actions">
                <button class="ghost-button action-button" @click="startEdit(rate)">编辑</button>
                <button class="ghost-button action-button" @click="toggleRate(rate.id, rate.status === 'active' ? 'paused' : 'active')">
                  {{ rate.status === 'active' ? '暂停' : '启用' }}
                </button>
                <button class="ghost-button action-button danger-action" @click="deleteRate(rate)">删除</button>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="selectorOpen" class="selector-mask" @click.self="closeCardSelector">
      <view class="selector-dialog">
        <view class="selector-head">
          <view>
            <text class="eyebrow">预设卡片</text>
            <text class="dialog-title">选择礼品卡品牌</text>
          </view>
          <button class="icon-button" title="关闭" @click="closeCardSelector">
            <image :src="uiIcons.close" mode="aspectFit" />
          </button>
        </view>

        <view class="selector-search">
          <input v-model.trim="cardSearch" class="search-input" placeholder="搜索名称或别名" focus />
          <text class="result-count">{{ filteredCards.length }} / {{ giftCardCatalog.length }}</text>
        </view>

        <scroll-view scroll-y class="card-option-list">
          <button
            v-for="card in filteredCards"
            :key="card.code"
            :class="['card-option', pendingCardCode === card.code && 'selected']"
            @click="pendingCardCode = card.code"
          >
            <image class="option-logo" :src="cardLogoFor(card.code, card.name)" mode="aspectFit" />
            <view class="option-copy">
              <text class="option-name">{{ card.name }}</text>
              <text class="option-code">{{ card.code }}</text>
            </view>
            <view :class="['selection-mark', pendingCardCode === card.code && 'checked']"></view>
          </button>

          <view v-if="!filteredCards.length" class="selector-empty">
            <text class="section-title">未找到卡片</text>
            <text class="panel-note">可以切换到手动输入，添加其他礼品卡。</text>
            <button class="ghost-button manual-link" @click="useManualEntry">使用手动输入</button>
          </view>
        </scroll-view>

        <view class="selector-footer">
          <button class="ghost-button footer-button" @click="closeCardSelector">取消</button>
          <button class="primary-button footer-button" :disabled="!pendingCardCode" @click="confirmCardSelection">确认选择</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import { useAppStore } from '@/store/app'
import type { CurrencyExchangeRateItem, RateItem } from '@/types'
import { fetchAdminCurrencyExchangeRates, updateAdminCurrencyExchangeRate } from '@/utils/api'
import { cardLogoFor, uiIcons } from '@/utils/art'
import { findGiftCardByCode, findGiftCardByName, giftCardCatalog, matchesGiftCardSearch } from '@/utils/gift-card-catalog'

type CardMode = 'preset' | 'manual'

const store = useAppStore()
const isAdminReady = ref(false)
const notice = ref('')
const editingId = ref('')
const cardMode = ref<CardMode>('preset')
const selectorOpen = ref(false)
const cardSearch = ref('')
const pendingCardCode = ref('')
const generalRates = ref<CurrencyExchangeRateItem[]>([])
const generalRateInputs = reactive<Record<string, string>>({})

const form = reactive({
  cardName: '',
  cardCode: null as string | null,
  region: defaultRegionCode(),
  rate: ''
})

const selectedCard = computed(() => findGiftCardByCode(form.cardCode))
const matchedManualCard = computed(() => findGiftCardByName(form.cardName))
const selectedCardLogo = computed(() => cardLogoFor(form.cardCode, form.cardName))
const filteredCards = computed(() => giftCardCatalog.filter((card) => matchesGiftCardSearch(card, cardSearch.value)))
const selectedRegionIndex = computed(() => {
  const index = store.state.countries.findIndex((country) => country.code === form.region)
  return index >= 0 ? index : 0
})
const selectedRegionLabel = computed(() => store.state.countries[selectedRegionIndex.value]?.name || '请选择地区')

async function submitRate() {
  const cardName = cardMode.value === 'preset' ? selectedCard.value?.name || '' : form.cardName.trim()
  if (!cardName) {
    notice.value = cardMode.value === 'preset' ? '请选择预设卡片。' : '请输入卡片名称。'
    return
  }
  if (!form.rate.trim()) {
    notice.value = '请输入汇率。'
    return
  }
  const localPayoutPerUsd = Number(form.rate)
  if (!Number.isFinite(localPayoutPerUsd) || localPayoutPerUsd <= 0) {
    notice.value = '汇率必须大于 0。'
    return
  }

  try {
    const payload = {
      cardName,
      cardCode: cardMode.value === 'preset' ? form.cardCode : null,
      region: form.region || defaultRegionCode(),
      rate: form.rate,
      localPayoutPerUsd
    }
    if (editingId.value) {
      await store.updateRate(editingId.value, payload)
      notice.value = '汇率已更新。'
    } else {
      await store.addRate(payload)
      notice.value = '汇率已创建。'
    }
    resetForm(false)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '提交失败'
  }
}

function setCardMode(mode: CardMode) {
  if (cardMode.value === mode) return
  cardMode.value = mode
  form.cardCode = null
  form.cardName = ''
  notice.value = ''
}

function openCardSelector() {
  pendingCardCode.value = form.cardCode || ''
  cardSearch.value = ''
  selectorOpen.value = true
}

function closeCardSelector() {
  selectorOpen.value = false
  pendingCardCode.value = ''
  cardSearch.value = ''
}

function confirmCardSelection() {
  const card = findGiftCardByCode(pendingCardCode.value)
  if (!card) return
  form.cardCode = card.code
  form.cardName = card.name
  closeCardSelector()
}

function useManualEntry() {
  closeCardSelector()
  setCardMode('manual')
}

function startEdit(rate: RateItem) {
  const preset = findGiftCardByCode(rate.cardCode) || findGiftCardByName(rate.cardName)
  editingId.value = rate.id
  cardMode.value = preset ? 'preset' : 'manual'
  form.cardCode = preset?.code || null
  form.cardName = preset?.name || rate.cardName
  form.region = resolveRegionCode(rate.region)
  form.rate = rate.localPayoutPerUsd || rate.rate.replace(/[^0-9.]/g, '')
  notice.value = `正在编辑 ${rate.cardName}。`
  uni.pageScrollTo({ scrollTop: 0, duration: 200 })
}

function resetForm(clearNotice = true) {
  editingId.value = ''
  cardMode.value = 'preset'
  form.cardCode = null
  form.cardName = ''
  form.region = defaultRegionCode()
  form.rate = ''
  if (clearNotice) notice.value = ''
}

function handleRegionChange(event: { detail: { value: number | string } }) {
  const country = store.state.countries[Number(event.detail.value)]
  if (country) form.region = country.code
}

function defaultRegionCode() {
  return store.state.countries[0]?.code || 'NG'
}

function resolveRegionCode(region: string) {
  const normalized = region.trim().replace(/^[+\uFF0B]/, '').replace(/[\s_-]+/g, '').toUpperCase()
  const aliases: Record<string, string> = {
    NG: 'NG', NIGERIA: 'NG', '234': 'NG', 尼日利亚: 'NG',
    IN: 'IN', INDIA: 'IN', '91': 'IN', 印度: 'IN',
    CM: 'CM', CAMEROON: 'CM', '237': 'CM', 喀麦隆: 'CM',
    GH: 'GH', GHANA: 'GH', '233': 'GH', 加纳: 'GH',
    KE: 'KE', KENYA: 'KE', '254': 'KE',
    US: 'US', USA: 'US', UNITEDSTATES: 'US', '1': 'US'
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

function deleteRate(rate: RateItem) {
  uni.showModal({
    title: '删除汇率',
    content: `确定删除 ${rate.cardName}（${rate.region}）的汇率吗？`,
    confirmText: '删除',
    confirmColor: '#E4002B',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await store.deleteRate(rate.id)
        if (editingId.value === rate.id) resetForm()
        notice.value = '汇率已删除。'
      } catch (error) {
        notice.value = error instanceof Error ? error.message : '删除失败'
      }
    }
  })
}

function requireAdmin() {
  if (store.state.currentUser?.roleCode === 'ADMIN') {
    isAdminReady.value = true
    return true
  }
  isAdminReady.value = false
  uni.redirectTo({ url: '/pages/admin-login/index' })
  return false
}

function goAdminConsole() { uni.redirectTo({ url: '/pages/admin-console/index' }) }
function goSupportChat() { uni.redirectTo({ url: '/pages/support-chat-v2/index' }) }
function goUserHome() { uni.redirectTo({ url: '/pages/home/index' }) }

async function loadGeneralRates() {
  try {
    generalRates.value = await fetchAdminCurrencyExchangeRates()
    generalRates.value.forEach((rate) => { generalRateInputs[rate.countryCode] = rate.localCurrencyPerUsd })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '通用货币汇率加载失败'
  }
}

async function saveGeneralRate(rate: CurrencyExchangeRateItem) {
  const value = Number(generalRateInputs[rate.countryCode])
  if (!Number.isFinite(value) || value <= 0) {
    notice.value = '通用货币汇率必须大于 0。'
    return
  }
  try {
    const updated = await updateAdminCurrencyExchangeRate({
      countryCode: rate.countryCode,
      localCurrencyPerUsd: value,
      enabled: rate.enabled,
      note: rate.note
    })
    const index = generalRates.value.findIndex((item) => item.countryCode === updated.countryCode)
    if (index >= 0) generalRates.value.splice(index, 1, updated)
    generalRateInputs[updated.countryCode] = updated.localCurrencyPerUsd
    notice.value = `${updated.countryName} 通用汇率已更新。`
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '通用货币汇率更新失败'
  }
}

onShow(() => {
  if (!requireAdmin()) return
  store.refreshRates().catch((error) => {
    notice.value = error instanceof Error ? error.message : '汇率加载失败'
  })
  loadGeneralRates()
})
</script>

<style scoped lang="scss">
.admin-rate-page { background: #f7f7f8; color: #111111; }
.page-stack { display: flex; flex-direction: column; gap: 20rpx; }
.general-rate-panel { padding: 26rpx; border: 1rpx solid #d9dde3; background: #ffffff; }
.general-rate-grid { margin-top: 18rpx; display: grid; gap: 12rpx; }
.general-rate-row { display: grid; grid-template-columns: minmax(0, 1fr) 220rpx auto; align-items: center; gap: 14rpx; padding: 14rpx 0; border-bottom: 1rpx solid #e7e8eb; }
.general-rate-row:last-child { border-bottom: 0; }
.general-rate-input { height: 68rpx; }
.admin-top-nav { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rpx; overflow: hidden; border: 1rpx solid #d9dde3; background: #d9dde3; }
.nav-button { margin: 0; min-height: 74rpx; padding: 16rpx 12rpx; border: 0; border-radius: 0; background: #ffffff; color: #111111; box-shadow: none; font-size: 24rpx; font-weight: 700; line-height: 1.2; }
.nav-button.active { background: #002fa7; color: #ffffff; }
.page-heading { padding: 30rpx; display: flex; align-items: flex-end; justify-content: space-between; gap: 24rpx; border: 1rpx solid #d9dde3; background: #ffffff; }
.page-heading .title, .dialog-title { display: block; margin-top: 8rpx; color: #111111; font-size: 38rpx; font-weight: 800; letter-spacing: 0; }
.page-heading .subtitle { display: block; margin-top: 8rpx; color: #667085; font-size: 24rpx; }
.heading-count { min-width: 130rpx; padding-left: 24rpx; border-left: 1rpx solid #d9dde3; text-align: right; }
.count-value { display: block; color: #002fa7; font-size: 52rpx; font-weight: 800; line-height: 1; }
.count-label { display: block; margin-top: 8rpx; color: #667085; font-size: 22rpx; }
.rate-workspace { display: grid; grid-template-columns: minmax(300px, 0.72fr) minmax(0, 1.28fr); gap: 20rpx; align-items: start; }
.editor-panel, .rates-panel { padding: 26rpx; border: 1rpx solid #d9dde3; background: #ffffff; }
.panel-heading { margin-bottom: 26rpx; display: flex; align-items: flex-start; justify-content: space-between; gap: 16rpx; }
.section-title { display: block; color: #111111; font-size: 28rpx; font-weight: 800; }
.panel-note { display: block; margin-top: 7rpx; color: #667085; font-size: 22rpx; line-height: 1.45; }
.text-button { width: auto; margin: 0; padding: 0; border: 0; background: transparent; color: #002fa7; font-size: 23rpx; font-weight: 700; line-height: 1.4; }
.field-label { display: block; margin-bottom: 10rpx; color: #344054; font-size: 23rpx; font-weight: 700; }
.field-gap { margin-top: 22rpx; }
.mode-switch { display: grid; grid-template-columns: 1fr 1fr; gap: 1rpx; padding: 1rpx; background: #d9dde3; }
.mode-button { margin: 0; padding: 16rpx; border: 0; border-radius: 0; background: #ffffff; color: #475467; box-shadow: none; font-size: 23rpx; font-weight: 700; }
.mode-button.active { background: #002fa7; color: #ffffff; }
.field-input, .card-select-trigger { width: 100%; min-height: 82rpx; box-sizing: border-box; border: 1rpx solid #cfd4dc; border-radius: 0; background: #ffffff; color: #111111; font-size: 25rpx; }
.field-input { padding: 0 20rpx; }
.card-select-trigger { margin: 0; padding: 12rpx 18rpx; display: flex; align-items: center; text-align: left; box-shadow: none; }
.selected-card-logo, .custom-preview-logo, .rate-logo, .option-logo { flex: 0 0 auto; background: #f7f7f8; border: 1rpx solid #e1e4e8; }
.selected-card-logo { width: 58rpx; height: 58rpx; }
.selected-card-copy { min-width: 0; flex: 1; margin-left: 16rpx; }
.selected-card-name { display: block; color: #111111; font-size: 25rpx; font-weight: 800; }
.selected-card-name.placeholder { color: #667085; font-weight: 600; }
.selected-card-code { display: block; margin-top: 4rpx; color: #667085; font-size: 19rpx; }
.select-chevron { width: 30rpx; height: 30rpx; }
.custom-preview { margin-top: 12rpx; padding: 14rpx; display: flex; align-items: center; gap: 14rpx; border-left: 4rpx solid #002fa7; background: #f7f7f8; }
.custom-preview-logo { width: 50rpx; height: 50rpx; }
.preview-title { display: block; color: #111111; font-size: 23rpx; font-weight: 700; }
.preview-note { display: block; margin-top: 3rpx; color: #667085; font-size: 19rpx; }
.select-field { display: flex; align-items: center; justify-content: space-between; }
.select-code { color: #002fa7; font-weight: 800; }
.submit-button { width: 100%; margin-top: 28rpx; border-radius: 0; background: #002fa7; }
.form-notice { display: block; margin-top: 14rpx; color: #475467; font-size: 22rpx; line-height: 1.45; }
.list-heading { padding-bottom: 20rpx; border-bottom: 1rpx solid #d9dde3; }
.empty-state { padding: 50rpx 20rpx; text-align: center; }
.rate-row { padding: 20rpx 0; display: flex; align-items: center; justify-content: space-between; gap: 20rpx; border-bottom: 1rpx solid #e7e9ed; }
.rate-row:last-child { border-bottom: 0; }
.rate-identity { min-width: 0; display: flex; align-items: center; gap: 16rpx; }
.rate-logo { width: 58rpx; height: 58rpx; }
.rate-copy { min-width: 0; }
.rate-name-line { display: flex; align-items: center; flex-wrap: wrap; gap: 8rpx; }
.rate-name { color: #111111; font-size: 25rpx; font-weight: 800; overflow-wrap: anywhere; }
.custom-badge, .status-badge { padding: 4rpx 8rpx; border: 1rpx solid #cfd4dc; color: #475467; font-size: 17rpx; font-weight: 700; }
.status-badge.active { border-color: #8acdaa; color: #18794e; }
.status-badge.paused { border-color: #e5b5ad; color: #b42318; }
.rate-meta { display: block; margin-top: 7rpx; color: #667085; font-size: 20rpx; }
.rate-side { min-width: 260rpx; display: flex; flex-direction: column; align-items: flex-end; gap: 12rpx; }
.rate-value { color: #111111; font-size: 24rpx; font-weight: 800; text-align: right; overflow-wrap: anywhere; }
.rate-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8rpx; }
.action-button { min-width: 92rpx; margin: 0; padding: 10rpx 14rpx; border-radius: 0; font-size: 20rpx; }
.danger-action { border-color: #e4002b; color: #e4002b; }
.selector-mask { position: fixed; inset: 0; z-index: 100; padding: 30rpx; display: flex; align-items: center; justify-content: center; box-sizing: border-box; background: rgba(17, 17, 17, 0.48); }
.selector-dialog { width: min(680rpx, 560px); max-height: min(840rpx, 82vh); display: flex; flex-direction: column; border: 1rpx solid #111111; background: #ffffff; }
.selector-head { padding: 24rpx; display: flex; align-items: flex-start; justify-content: space-between; gap: 20rpx; border-bottom: 1rpx solid #d9dde3; }
.dialog-title { font-size: 30rpx; }
.icon-button { width: 62rpx; height: 62rpx; margin: 0; padding: 16rpx; display: flex; border: 1rpx solid #d9dde3; border-radius: 0; background: #ffffff; box-shadow: none; }
.icon-button image { width: 100%; height: 100%; }
.selector-search { padding: 18rpx 24rpx; display: flex; align-items: center; gap: 14rpx; border-bottom: 1rpx solid #d9dde3; }
.search-input { min-width: 0; height: 68rpx; padding: 0 16rpx; flex: 1; box-sizing: border-box; border: 1rpx solid #cfd4dc; font-size: 23rpx; }
.result-count { color: #667085; font-size: 20rpx; font-variant-numeric: tabular-nums; }
.card-option-list { min-height: 260rpx; flex: 1; }
.card-option { width: 100%; min-height: 88rpx; margin: 0; padding: 13rpx 24rpx; display: flex; align-items: center; border: 0; border-bottom: 1rpx solid #e7e9ed; border-radius: 0; background: #ffffff; box-shadow: none; text-align: left; }
.card-option.selected { background: #eef3ff; box-shadow: inset 4rpx 0 0 #002fa7; }
.option-logo { width: 56rpx; height: 56rpx; }
.option-copy { min-width: 0; margin-left: 15rpx; flex: 1; }
.option-name { display: block; color: #111111; font-size: 24rpx; font-weight: 800; }
.option-code { display: block; margin-top: 3rpx; color: #667085; font-size: 18rpx; }
.selection-mark { width: 24rpx; height: 24rpx; box-sizing: border-box; border: 2rpx solid #98a2b3; border-radius: 50%; }
.selection-mark.checked { border: 7rpx solid #002fa7; background: #ffffff; }
.selector-empty { padding: 50rpx 24rpx; text-align: center; }
.manual-link { width: auto; margin-top: 20rpx; border-radius: 0; }
.selector-footer { padding: 18rpx 24rpx; display: grid; grid-template-columns: 1fr 1fr; gap: 12rpx; border-top: 1rpx solid #d9dde3; }
.footer-button { width: 100%; margin: 0; border-radius: 0; }

@media (max-width: 900px) {
  .rate-workspace { grid-template-columns: 1fr; }
}

@media (max-width: 620px) {
  .admin-top-nav { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .page-heading { align-items: flex-start; }
  .page-heading .title { font-size: 32rpx; }
  .heading-count { min-width: 100rpx; }
  .editor-panel, .rates-panel { padding: 22rpx; }
  .rate-row { align-items: flex-start; flex-direction: column; }
  .rate-side { min-width: 0; width: 100%; align-items: flex-start; }
  .rate-value { text-align: left; }
  .rate-actions { width: 100%; justify-content: flex-start; }
  .selector-mask { padding: 0; align-items: flex-end; }
  .selector-dialog { width: 100%; max-height: 88vh; border-right: 0; border-bottom: 0; border-left: 0; }
}
</style>
