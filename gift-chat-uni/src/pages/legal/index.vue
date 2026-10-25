<template>
  <view class="legal-page">
    <button class="legal-back" @click="back">Back</button>
    <text class="legal-title">{{ document.title }}</text>
    <text class="legal-updated">Updated {{ legal.updated }}</text>
    <view v-for="section in document.sections" :key="section.title" class="legal-section">
      <text class="legal-heading">{{ section.title }}</text>
      <text class="legal-body">{{ section.body }}</text>
    </view>
  </view>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import legal from '@/data/legal.json'
const kind = ref<'privacy' | 'terms'>('privacy')
const document = computed(() => legal[kind.value])
onLoad((options) => { kind.value = options?.kind === 'terms' ? 'terms' : 'privacy' })
function back() { uni.navigateBack({ fail: () => uni.reLaunch({ url: '/pages/login/index' }) }) }
</script>
<style scoped>
.legal-page { box-sizing: border-box; max-width: 760px; margin: 0 auto; padding: max(24px, env(safe-area-inset-top)) 24px 64px; background: var(--cb-surface); color: var(--cb-ink); }
.legal-back { width: max-content; margin: 0 0 24px; padding: 0 16px; min-height: 44px; border-radius: 10px; background: var(--cb-canvas); color: var(--cb-accent); font-size: 14px; }
.legal-back::after { border: 0; } .legal-back:hover { opacity: .8; } .legal-back:focus-visible { outline: 2px solid var(--cb-accent); }
.legal-title { display: block; font-size: 28px; font-weight: 700; line-height: 1.3; }
.legal-updated { display: block; margin-top: 12px; color: var(--cb-muted); font-size: 13px; }
.legal-section { margin-top: 28px; }
.legal-heading { display: block; font-size: 18px; font-weight: 600; }
.legal-body { display: block; margin-top: 8px; font-size: 14px; line-height: 1.8; white-space: pre-line; overflow-wrap: anywhere; }
</style>
