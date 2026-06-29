<template>
  <view class="page-shell sell-page">
    <view class="sell-head">
      <text class="back" @click="goBack">‹</text>
      <text class="page-title">Sell card</text>
      <view style="width: 48rpx"></view>
    </view>

    <view class="card-picker surface-card">
      <image class="card-icon" :src="cardLogoFor(activeRate?.cardName || 'Razer Gold')" mode="aspectFit" />
      <text class="card-name">{{ form.cardName }}</text>
      <button class="change-button" @click="chooseCard">Change</button>
    </view>

    <view class="form-panel">
      <view class="form-row">
        <text class="form-label">Country</text>
        <view class="select-field" @click="chooseCardCountry">
          <text>{{ form.cardCountry }}</text>
          <text class="chevron">⌄</text>
        </view>
      </view>

      <view class="form-row">
        <text class="form-label">Balance</text>
        <input v-model="balanceText" class="text-field" type="number" placeholder="100" />
      </view>

      <view class="amount-row">
        <button v-for="amount in quickAmounts" :key="amount" class="amount-chip" @click="setAmount(amount)">
          {{ amount }}
        </button>
      </view>

      <view class="form-row">
        <text class="form-label">Quantity</text>
        <view class="stepper">
          <button class="step-button" @click="changeQuantity(-1)">-</button>
          <text class="quantity">{{ form.quantity }}</text>
          <button class="step-button plus" @click="changeQuantity(1)">+</button>
        </view>
      </view>

      <view class="form-row">
        <text class="form-label">Type</text>
        <view class="segment-row">
          <button
            v-for="type in cardTypes"
            :key="type"
            :class="['segment-button', form.cardType === type && 'active-segment']"
            @click="form.cardType = type"
          >
            {{ type }}
          </button>
        </view>
      </view>

      <view class="form-row">
        <text class="form-label">Speed</text>
        <view class="segment-row">
          <button
            v-for="speed in speeds"
            :key="speed"
            :class="['segment-button', form.speed === speed && 'active-segment']"
            @click="form.speed = speed"
          >
            {{ speed }}
          </button>
        </view>
      </view>

      <view>
        <text class="block-label">Card Data</text>
        <input v-model="form.cardData" class="full-input" placeholder="Code Optional" />
      </view>

      <view>
        <text class="block-label">Upload Photo</text>
        <view class="upload-box" @click="uploadVoucher">
          <text class="camera">▣</text>
          <text class="upload-copy">{{ form.voucherImageUrl ? 'Photo ready' : 'Upload card image' }}</text>
        </view>
      </view>
    </view>

    <view class="settlement-bar">
      <view>
        <text class="settlement-label">Settlement Amount:</text>
        <text class="settlement-value">{{ settlementAmount }}</text>
      </view>
      <button class="confirm-button" @click="confirmSell">Confirm</button>
    </view>

    <text v-if="notice" class="notice-text">{{ notice }}</text>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { uploadImage } from '@/utils/api'
import { cardLogoFor } from '@/utils/art'

const store = useAppStore()
const notice = ref('')
const rateId = ref('')
const balanceText = ref('100')
const quickAmounts = [50, 100, 200, 500]
const cardTypes = ['Physical', 'Code', 'Horizontal Image', 'Whiteboard']
const speeds = ['Fast', 'Slow']
const cardCountries = ['AUD', 'USD', 'EUR', 'GBP']

const form = reactive({
  cardName: 'Razer Gold',
  cardCountry: 'AUD',
  settlementCountry: store.state.selectedCountryCode,
  quantity: 1,
  cardType: 'Physical',
  speed: 'Fast',
  cardData: '',
  voucherImageUrl: ''
})

const activeRate = computed(() => store.state.rates.find((rate) => rate.id === rateId.value) || store.state.rates[0])

onLoad((query) => {
  rateId.value = typeof query?.rateId === 'string' ? query.rateId : ''
})

onShow(async () => {
  await store.bootstrap()
  if (activeRate.value) {
    form.cardName = activeRate.value.cardName
    form.settlementCountry = activeRate.value.region
  }
})

const numericRate = computed(() => {
  const value = activeRate.value?.rate || ''
  const digits = value.match(/[\d.]+(?=\s*$)|[\d.]+/g)
  return Number(digits?.[digits.length - 1] || '0')
})

const settlementAmount = computed(() => {
  const total = Number(balanceText.value || '0') * form.quantity * numericRate.value
  return currencyPrefix(form.settlementCountry) + total.toLocaleString('en-US', { maximumFractionDigits: 2 })
})

function currencyPrefix(country: string) {
  if (country === 'IN') return '₹'
  if (country === 'CM') return 'XAF '
  if (country === 'GH') return 'GH₵'
  return '₦'
}

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
  const rates = store.state.rates.filter((rate) => rate.region === store.state.selectedCountryCode)
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
  try {
    const result = await chooseImageOnce()
    if (!result) return
    const asset = await uploadImage(result)
    form.voucherImageUrl = asset.publicUrl
    notice.value = 'Photo uploaded.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Upload failed'
  }
}

async function confirmSell() {
  if (!activeRate.value) return
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
    uni.setStorageSync('pending-support-draft', buildSellCardDraft(transaction.orderNo))
    uni.redirectTo({ url: '/pages/support-chat-v2/index' })
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Sell failed'
  }
}

function buildSellCardDraft(orderNo: string) {
  return [
    `Sell order ${orderNo}`,
    `Card: ${form.cardName}`,
    `Country: ${form.cardCountry}`,
    `Settlement country: ${form.settlementCountry}`,
    `Face value: ${balanceText.value || '0'} ${form.cardCountry} x${form.quantity}`,
    `Type: ${form.cardType}`,
    `Speed: ${form.speed}`,
    `Rate: ${activeRate.value?.rate || '-'}`,
    `Settlement: ${settlementAmount.value}`,
    form.cardData ? `Card data: ${form.cardData}` : '',
    form.voucherImageUrl ? `Voucher: ${form.voucherImageUrl}` : ''
  ].filter(Boolean).join('\n')
}

function chooseImageOnce() {
  return new Promise<string | null>((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      success(result) {
        resolve(result.tempFilePaths?.[0] || null)
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
  padding-bottom: 180rpx;
  background: linear-gradient(180deg, #9cf5ba 0%, #f7fff9 26%, #ffffff 100%);
}

.sell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 34rpx;
}

.back {
  width: 48rpx;
  font-size: 66rpx;
  line-height: 1;
  color: #1b1b1b;
}

.page-title {
  font-size: 38rpx;
  font-weight: 900;
}

.card-picker {
  margin-top: 34rpx;
  min-height: 132rpx;
  padding: 22rpx 26rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  border-radius: 10rpx;
}

.card-icon {
  width: 76rpx;
  height: 76rpx;
}

.card-name {
  flex: 1;
  font-size: 36rpx;
  font-weight: 900;
}

.change-button,
.confirm-button {
  min-width: 210rpx;
  height: 78rpx;
  border-radius: 10rpx;
  background: #08c967;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 78rpx;
}

.form-panel {
  margin-top: 48rpx;
  display: flex;
  flex-direction: column;
  gap: 36rpx;
}

.form-row {
  display: grid;
  grid-template-columns: 180rpx 1fr;
  align-items: center;
  gap: 24rpx;
}

.form-label,
.block-label {
  font-size: 30rpx;
  font-weight: 900;
  color: #202020;
}

.block-label {
  display: block;
  margin-bottom: 18rpx;
}

.select-field,
.text-field,
.full-input {
  min-height: 86rpx;
  padding: 0 28rpx;
  border-radius: 8rpx;
  background: #f7f7f7;
  font-size: 30rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chevron {
  font-size: 36rpx;
}

.amount-row {
  margin-left: 204rpx;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22rpx;
}

.amount-chip,
.step-button,
.segment-button {
  height: 66rpx;
  border-radius: 8rpx;
  background: #f5f5f5;
  color: #222;
  font-size: 28rpx;
  font-weight: 800;
  line-height: 66rpx;
}

.stepper,
.segment-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  flex-wrap: wrap;
}

.step-button {
  width: 68rpx;
  min-width: 68rpx;
}

.plus,
.active-segment {
  background: #08c967;
  color: #ffffff;
}

.quantity {
  min-width: 82rpx;
  height: 66rpx;
  border-radius: 8rpx;
  background: #f4f2f7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 900;
}

.segment-button {
  min-width: 190rpx;
  padding: 0 22rpx;
}

.upload-box {
  width: 230rpx;
  min-height: 180rpx;
  border-radius: 18rpx;
  border: 2rpx dashed #aeeecd;
  background: #f6fffb;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
}

.camera {
  width: 70rpx;
  height: 70rpx;
  border-radius: 18rpx;
  background: #08c967;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
}

.upload-copy {
  font-size: 22rpx;
  color: #6b747c;
}

.settlement-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  min-height: 122rpx;
  padding: 20rpx 34rpx 30rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(16, 24, 40, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22rpx;
}

.settlement-label {
  display: block;
  font-size: 24rpx;
  color: #8a8a8a;
  font-weight: 800;
}

.settlement-value {
  display: block;
  margin-top: 6rpx;
  font-size: 42rpx;
  color: #08c967;
  font-weight: 900;
}

.notice-text {
  display: block;
  margin-top: 24rpx;
  text-align: center;
  color: #606872;
  font-size: 24rpx;
}
</style>
