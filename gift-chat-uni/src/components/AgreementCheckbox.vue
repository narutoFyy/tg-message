<template>
  <view class="agreement">
    <checkbox-group @change="change">
      <label class="check-label"><checkbox value="agree" :checked="modelValue" color="#2368c4" /><text>I agree to the</text></label>
    </checkbox-group>
    <button class="legal-link" @click="openLegal('terms')">Terms of Use</button>
    <text>and</text>
    <button class="legal-link" @click="openLegal('privacy')">Privacy Policy</button><text>.</text>
  </view>
</template>
<script setup lang="ts">
import { openLegal } from '@/utils/privacy'
defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (event: 'update:modelValue', value: boolean): void }>()
function change(event: { detail: { value: string[] } }) { emit('update:modelValue', event.detail.value.includes('agree')) }
</script>
<style scoped>
.agreement { display: flex; flex-wrap: wrap; align-items: center; gap: 4px; margin-top: 20px; color: inherit; font-size: 14px; line-height: 1.6; }
.check-label { display: flex; align-items: center; gap: 6px; min-height: 44px; }
.legal-link { display: inline; margin: 0; padding: 4px 0; background: transparent; color: var(--register-gold, var(--cb-accent)); font-size: 14px; line-height: 1.6; text-decoration: underline; border-radius: 4px; }
.legal-link::after { border: 0; }
.legal-link:hover { opacity: .8; }
.legal-link:focus-visible { outline: 2px solid currentColor; outline-offset: 2px; }
</style>
