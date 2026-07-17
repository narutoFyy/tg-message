<template>
  <view class="page-shell sell-page">
    <view class="sell-content">
      <view class="page-header sell-head">
        <view class="head-title">
          <image class="back-icon" :src="uiIcons.back" mode="aspectFit" @click="goBack" />
          <view>
            <text class="eyebrow">New order</text>
            <text class="title">Sell gift card</text>
          </view>
        </view>
        <text class="rate-reference">{{ activeRate?.rate || 'Rate unavailable' }}</text>
      </view>

      <view class="card-picker tone-market">
        <image class="card-icon" :src="cardLogoFor(activeRate?.cardCode, activeRate?.cardName || 'Razer Gold')" mode="aspectFit" />
        <view class="card-copy">
          <text class="card-name">{{ form.cardName }}</text>
          <text class="muted">Settles in {{ accountCountryName }} ({{ accountCurrencyCode }})</text>
        </view>
        <button class="change-button" @click="chooseCard">Change</button>
      </view>

      <view class="form-section value-section">
        <view class="section-heading">
          <text class="section-index">01</text>
          <text class="section-title">Card value</text>
        </view>
        <view class="form-grid">
          <view class="form-field">
            <text class="field-label">Card currency</text>
            <view class="field-input select-field" @click="chooseCardCountry">
              <text>{{ form.cardCountry }}</text>
              <text class="field-action">Change</text>
            </view>
          </view>
          <view class="form-field">
            <text class="field-label">Face value</text>
            <input v-model="balanceText" class="field-input" type="number" placeholder="100" />
          </view>
        </view>
        <view class="amount-row">
          <button
            v-for="amount in quickAmounts"
            :key="amount"
            :class="['amount-chip', Number(balanceText) === amount && 'active-chip']"
            @click="setAmount(amount)"
          >{{ amount }}</button>
        </view>
        <view class="quantity-row">
          <text class="field-label quantity-label">Quantity</text>
          <view class="stepper">
            <button class="step-button" @click="changeQuantity(-1)">-</button>
            <text class="quantity">{{ form.quantity }}</text>
            <button class="step-button" @click="changeQuantity(1)">+</button>
          </view>
        </view>
      </view>

      <view class="form-section details-section">
        <view class="section-heading">
          <text class="section-index">02</text>
          <text class="section-title">Card details</text>
        </view>
        <view class="form-field">
          <text class="field-label">Card type</text>
          <view class="segment-row">
            <button
              v-for="type in cardTypes"
              :key="type"
              :class="['segment-button', form.cardType === type && 'active-segment']"
              @click="form.cardType = type"
            >{{ type }}</button>
          </view>
        </view>
        <view class="form-field form-gap">
          <text class="field-label">Processing speed</text>
          <view class="segment-row compact-segments">
            <button
              v-for="speed in speeds"
              :key="speed"
              :class="['segment-button', form.speed === speed && 'active-segment']"
              @click="form.speed = speed"
            >{{ speed }}</button>
          </view>
        </view>
        <view class="form-field form-gap">
          <text class="field-label">Card code</text>
          <input v-model="form.cardData" class="field-input" placeholder="Optional card code" />
        </view>
      </view>

      <view class="form-section upload-section proof-section">
        <view class="section-heading">
          <text class="section-index">03</text>
          <text class="section-title">Proof of card</text>
        </view>
        <view :class="['upload-row', form.voucherImageUrl && 'upload-ready', uploading && 'uploading']" @click="uploadVoucher">
          <image v-if="form.voucherImageUrl" class="upload-preview" :src="form.voucherImageUrl" mode="aspectFill" />
          <view v-else class="upload-mark"></view>
          <view class="upload-copy-wrap">
            <text class="upload-title">{{ uploading ? 'Uploading image' : form.voucherImageUrl ? 'Image attached' : 'Attach card image' }}</text>
            <text class="muted">{{ uploading ? 'Please wait...' : form.voucherImageUrl ? 'Tap to replace the image' : 'Choose one clear image from your device' }}</text>
          </view>
          <text class="field-action">{{ uploading ? 'Uploading' : form.voucherImageUrl ? 'Replace' : 'Choose' }}</text>
        </view>
      </view>

      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>

    <view class="settlement-bar">
      <view>
        <text class="settlement-label">Estimated settlement</text>
        <text class="settlement-value">{{ settlementAmount }}</text>
      </view>
      <button class="confirm-button" @click="confirmSell">Create order</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { uploadImage } from '@/utils/api'
import { cardLogoFor, uiIcons } from '@/utils/art'
import type { TransactionItem } from '@/types'

const store = useAppStore()
const notice = ref('')
const uploading = ref(false)
const rateId = ref('')
const balanceText = ref('100')
const quickAmounts = [50, 100, 200, 500]
const cardTypes = ['Physical', 'Code', 'Horizontal Image', 'Whiteboard']
const speeds = ['Fast', 'Slow']
const cardCountries = ['AUD', 'USD', 'EUR', 'GBP']

const form = reactive({
  cardName: 'Razer Gold',
  cardCountry: 'AUD',
  settlementCountry: store.state.currentUser?.countryCode || store.state.selectedCountryCode,
  quantity: 1,
  cardType: 'Physical',
  speed: 'Fast',
  cardData: '',
  voucherImageUrl: ''
})

const accountCountryCode = computed(() => store.state.currentUser?.countryCode || store.state.selectedCountryCode)
const accountCountryName = computed(() => store.state.currentUser?.countryName || store.selectedCountry().name)
const accountCurrencyCode = computed(() => store.state.currentUser?.currencyCode || 'USD')
const accountCurrencySymbol = computed(() => store.state.currentUser?.currencySymbol || accountCurrencyCode.value)
const accountRates = computed(() => store.state.rates.filter((rate) => rate.region === accountCountryCode.value))
const activeRate = computed(() => accountRates.value.find((rate) => rate.id === rateId.value) || accountRates.value[0])

onLoad((query) => {
  rateId.value = typeof query?.rateId === 'string' ? query.rateId : ''
})

onShow(async () => {
  await store.bootstrap()
  form.settlementCountry = accountCountryCode.value
  if (activeRate.value) {
    form.cardName = activeRate.value.cardName
    form.settlementCountry = activeRate.value.region
  }
})

const numericRate = computed(() => {
  const structuredRate = Number(activeRate.value?.localPayoutPerUsd || '0')
  if (structuredRate > 0) return structuredRate
  const value = activeRate.value?.rate || ''
  const digits = value.match(/[\d.]+(?=\s*$)|[\d.]+/g)
  return Number(digits?.[digits.length - 1] || '0')
})

const settlementAmount = computed(() => {
  const total = Number(balanceText.value || '0') * form.quantity * numericRate.value
  const separator = accountCurrencySymbol.value.length > 1 ? ' ' : ''
  return `${accountCurrencySymbol.value}${separator}${total.toLocaleString('en-US', { maximumFractionDigits: 2 })}`
})

function setAmount(amount: number) {
  balanceText.value = String(amount)
}

function changeQuantity(delta: number) {
  form.quantity = Math.max(1, form.quantity + delta)
}

function chooseCardCountry() {
  uni.showActionSheet({
    itemList: cardCountries,
    success(result) {
      form.cardCountry = cardCountries[result.tapIndex] || form.cardCountry
    }
  })
}

function chooseCard() {
  const rates = accountRates.value
  if (!rates.length) {
    notice.value = 'No rates are available for this country.'
    return
  }
  uni.showActionSheet({
    itemList: rates.map((rate) => rate.cardName),
    success(result) {
      const rate = rates[result.tapIndex]
      if (!rate) return
      rateId.value = rate.id
      form.cardName = rate.cardName
      form.settlementCountry = rate.region
    }
  })
}

async function uploadVoucher() {
  if (uploading.value) return
  uploading.value = true
  notice.value = ''
  try {
    const result = await chooseImageOnce()
    if (!result) return
    const asset = await uploadImage(result)
    form.voucherImageUrl = asset.publicUrl
    notice.value = 'Photo uploaded.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Upload failed'
  } finally {
    uploading.value = false
  }
}

async function confirmSell() {
  if (!activeRate.value) {
    notice.value = 'Choose an available card rate first.'
    return
  }
  try {
    const transaction = await store.createSellOrder({
      cardName: form.cardName,
      cardCountry: form.cardCountry,
      settlementCountry: form.settlementCountry,
      faceValue: Number(balanceText.value || '0'),
      quantity: form.quantity,
      rate: activeRate.value.rate,
      settlementAmount: settlementAmount.value,
      cardType: form.cardType,
      speed: form.speed,
      cardData: form.cardData || undefined,
      voucherImageUrl: form.voucherImageUrl || undefined,
      sendChatMessage: false
    })
    uni.setStorageSync('pending-support-draft', buildSellCardDraft(transaction))
    if (form.voucherImageUrl) {
      uni.setStorageSync('pending-support-image', form.voucherImageUrl)
    } else {
      uni.removeStorageSync('pending-support-image')
    }
    uni.redirectTo({ url: '/pages/support/index' })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Sell failed'
  }
}

function buildSellCardDraft(transaction: TransactionItem) {
  return [
    `Sell order ${transaction.orderNo}`,
    `Card: ${form.cardName}`,
    `Country: ${form.cardCountry}`,
    `Settlement country: ${accountCountryName.value}`,
    `Face value: ${balanceText.value || '0'} ${form.cardCountry} x${form.quantity}`,
    `Type: ${form.cardType}`,
    `Speed: ${form.speed}`,
    `Rate: ${transaction.businessRate ? `$1 ≈ ${transaction.businessRate} ${transaction.currencyCode}` : activeRate.value?.rate || '-'}`,
    `Settlement: ${transaction.payoutAmount}`,
    form.cardData ? `Card data: ${form.cardData}` : '',
    form.voucherImageUrl ? 'Voucher: Image attached below' : ''
  ].filter(Boolean).join('\n')
}

function chooseImageOnce() {
  return new Promise<string | File | null>((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success(result) {
        const files = Array.isArray(result.tempFiles) ? result.tempFiles : result.tempFiles ? [result.tempFiles] : []
        const firstFile = files[0]
        if (typeof File !== 'undefined' && firstFile instanceof File) {
          resolve(firstFile)
          return
        }
        const paths = Array.isArray(result.tempFilePaths) ? result.tempFilePaths : [result.tempFilePaths]
        const objectPath = firstFile && 'path' in firstFile ? firstFile.path : ''
        resolve(objectPath || paths[0] || null)
      },
      fail(error) {
        reject(error)
      }
    })
  })
}

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped lang="scss">
.sell-page {
  min-height: 100vh;
  min-height: 100dvh;
  padding-bottom: calc(190rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(190rpx + env(safe-area-inset-bottom));
  background: #f7f7f8;
}

.sell-content {
  width: 100%;
  max-width: 1040rpx;
  margin: 0 auto;
}

.sell-head {
  align-items: center;
}

.head-title {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.head-title .eyebrow,
.head-title .title {
  display: block;
}

.head-title .title {
  margin-top: 4rpx;
  font-size: 34rpx;
}

.back-icon {
  width: 42rpx;
  height: 42rpx;
  flex: 0 0 auto;
}

.rate-reference {
  max-width: 270rpx;
  color: #002fa7;
  font-size: 24rpx;
  font-weight: 700;
  text-align: right;
}

.card-picker {
  min-height: 112rpx;
  margin-top: 28rpx;
  padding: 18rpx 22rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 18rpx;
  border-left: 5rpx solid var(--cb-sky-strong);
}

.card-icon {
  width: 64rpx;
  height: 64rpx;
  flex: 0 0 auto;
}

.card-copy {
  min-width: 0;
  flex: 1;
}

.card-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #111111;
  font-size: 29rpx;
  font-weight: 700;
}

.change-button {
  min-width: 112rpx;
  height: 58rpx;
  padding: 0 16rpx;
  border: 1rpx solid #c8c9cf;
  border-radius: 5rpx;
  background: #ffffff;
  color: #002fa7;
  font-size: 23rpx;
  font-weight: 700;
  line-height: 56rpx;
}

.change-button::after,
.amount-chip::after,
.step-button::after,
.segment-button::after,
.confirm-button::after {
  border: 0;
}

.form-section {
  margin-top: 26rpx;
  padding: 26rpx 26rpx 28rpx;
  background: #ffffff;
  border: 1rpx solid var(--cb-line);
  border-radius: 12rpx;
  box-shadow: 0 8rpx 24rpx rgba(34, 54, 74, 0.04);
}

.value-section {
  background: var(--cb-sky);
  border-color: #cfe4fb;
}

.details-section {
  background: var(--cb-lilac);
  border-color: #ddd5fb;
}

.proof-section {
  background: var(--cb-mint);
  border-color: #c6ead8;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 24rpx;
}

.section-index {
  color: var(--cb-accent);
  font-size: 21rpx;
  font-weight: 700;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.form-field {
  min-width: 0;
}

.form-gap {
  margin-top: 24rpx;
}

.select-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.field-action {
  color: #002fa7;
  font-size: 22rpx;
  font-weight: 700;
}

.amount-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10rpx;
  margin-top: 14rpx;
}

.amount-chip,
.step-button,
.segment-button {
  height: 62rpx;
  padding: 0 14rpx;
  border: 1rpx solid #dedfe3;
  border-radius: 5rpx;
  background: #f7f7f8;
  color: #111111;
  font-size: 23rpx;
  font-weight: 700;
  line-height: 60rpx;
}

.active-chip,
.active-segment {
  border-color: var(--cb-accent);
  background: #ffffff;
  color: var(--cb-accent);
}

.quantity-row {
  margin-top: 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.quantity-label {
  margin-bottom: 0;
}

.stepper {
  display: grid;
  grid-template-columns: 62rpx 72rpx 62rpx;
  align-items: center;
}

.stepper > * {
  border-radius: 0;
}

.quantity {
  height: 62rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1rpx solid #dedfe3;
  border-bottom: 1rpx solid #dedfe3;
  background: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
}

.segment-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10rpx;
}

.compact-segments {
  grid-template-columns: repeat(2, minmax(0, 180rpx));
}

.segment-button {
  width: 100%;
  height: auto;
  min-height: 66rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1.25;
  white-space: normal;
}

.upload-row {
  min-height: 94rpx;
  padding: 16rpx 18rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  border: 1rpx dashed #9a9ca3;
  background: rgba(255, 255, 255, 0.74);
}

.upload-ready {
  border-style: solid;
  border-color: #137a4e;
  background: #eaf6f0;
}

.uploading {
  pointer-events: none;
  opacity: 0.72;
}

.upload-preview {
  width: 72rpx;
  height: 72rpx;
  flex: 0 0 auto;
  border: 1rpx solid #a9d8bf;
  background: #ffffff;
}

.upload-mark {
  width: 34rpx;
  height: 34rpx;
  border: 2rpx solid #002fa7;
  position: relative;
  flex: 0 0 auto;
}

.upload-mark::before,
.upload-mark::after {
  content: "";
  position: absolute;
  left: 50%;
  top: 50%;
  background: #002fa7;
  transform: translate(-50%, -50%);
}

.upload-mark::before {
  width: 18rpx;
  height: 2rpx;
}

.upload-mark::after {
  width: 2rpx;
  height: 18rpx;
}

.upload-copy-wrap {
  min-width: 0;
  flex: 1;
}

.upload-title {
  display: block;
  margin-bottom: 3rpx;
  color: #111111;
  font-size: 24rpx;
  font-weight: 700;
}

.notice-text {
  display: block;
  margin-top: 22rpx;
  color: #b42318;
  font-size: 24rpx;
  text-align: center;
}

.settlement-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  min-height: calc(126rpx + constant(safe-area-inset-bottom));
  min-height: calc(126rpx + env(safe-area-inset-bottom));
  padding: 18rpx max(28rpx, calc((100vw - 960px) / 2)) calc(18rpx + constant(safe-area-inset-bottom));
  padding: 18rpx max(28rpx, calc((100vw - 960px) / 2)) calc(18rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22rpx;
  background: #ffffff;
  border-top: 1rpx solid #c8c9cf;
  z-index: 20;
}

.settlement-bar > view {
  min-width: 0;
  flex: 1;
}

.settlement-label,
.settlement-value {
  display: block;
}

.settlement-label {
  color: #6f7178;
  font-size: 21rpx;
}

.settlement-value {
  margin-top: 5rpx;
  color: #002fa7;
  font-size: 34rpx;
  font-weight: 700;
}

.confirm-button {
  flex: 0 0 auto;
  min-width: 220rpx;
  height: 78rpx;
  padding: 0 22rpx;
  border: 0;
  border-radius: 5rpx;
  background: #002fa7;
  color: #ffffff;
  font-size: 27rpx;
  font-weight: 700;
  line-height: 78rpx;
}

@media (max-width: 480px) {
  .sell-page {
    padding-left: calc(20rpx + constant(safe-area-inset-left));
    padding-left: calc(20rpx + env(safe-area-inset-left));
    padding-right: calc(20rpx + constant(safe-area-inset-right));
    padding-right: calc(20rpx + env(safe-area-inset-right));
  }

  .sell-head {
    align-items: flex-start;
    gap: 16rpx;
  }

  .rate-reference {
    max-width: 220rpx;
    font-size: 21rpx;
  }

  .card-picker {
    margin-top: 20rpx;
  }

  .form-section {
    margin-top: 20rpx;
    padding: 22rpx 20rpx 24rpx;
  }

  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .amount-row {
    gap: 8rpx;
  }

  .amount-chip,
  .step-button,
  .segment-button {
    padding-left: 8rpx;
    padding-right: 8rpx;
  }

  .upload-row {
    padding: 14rpx;
    gap: 12rpx;
  }

  .settlement-bar {
    gap: 14rpx;
    padding-left: calc(20rpx + constant(safe-area-inset-left));
    padding-left: calc(20rpx + env(safe-area-inset-left));
    padding-right: calc(20rpx + constant(safe-area-inset-right));
    padding-right: calc(20rpx + env(safe-area-inset-right));
  }

  .settlement-label {
    font-size: 19rpx;
  }

  .settlement-value {
    font-size: 30rpx;
  }

  .confirm-button {
    min-width: 190rpx;
    height: 72rpx;
    padding: 0 18rpx;
    font-size: 25rpx;
    line-height: 72rpx;
  }
}

@media (max-width: 420px) {
  .rate-reference {
    display: none;
  }

  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .settlement-value {
    font-size: 30rpx;
  }

  .confirm-button {
    min-width: 190rpx;
  }
}

@media (min-width: 768px) {
  .sell-content {
    max-width: 960px;
  }

  .segment-row {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .compact-segments {
    grid-template-columns: repeat(2, minmax(0, 180rpx));
  }
}
</style>
