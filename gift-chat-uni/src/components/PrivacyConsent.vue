<template>
  <view v-if="!privacyAccepted" class="privacy-overlay" @touchmove.stop.prevent>
    <view class="privacy-dialog" role="dialog" aria-modal="true" aria-label="Your privacy at Xcard">
      <text class="privacy-title">Your privacy at Xcard</text>
      <text class="privacy-copy">Please read our Terms of Use and Privacy Policy before using Xcard.</text>
      <text class="privacy-copy">We process account details, messages and transaction information to provide the service. Camera and microphone access is requested only when you use those features. Android photos and videos are selected through the system picker.</text>
      <view class="privacy-links">
        <button class="privacy-link" @click="openLegal('terms')">Terms of Use</button>
        <button class="privacy-link" @click="openLegal('privacy')">Privacy Policy</button>
      </view>
      <text v-if="declined" class="privacy-copy decline-copy">You have not agreed. Sign-in and account services remain paused. You can read the documents and decide when you are ready.</text>
      <button class="privacy-accept" @click="acceptPrivacy">Agree and continue</button>
      <button class="privacy-refuse" @click="declined = true">Disagree</button>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { privacyAccepted, acceptPrivacy, openLegal } from '@/utils/privacy'
const declined = ref(false)
</script>
<style scoped>
.privacy-overlay { position: fixed; inset: 0; z-index: 9999; display: flex; align-items: center; justify-content: center; padding: 24px; box-sizing: border-box; background: rgba(15,23,42,.55); }
.privacy-dialog { width: 100%; max-width: 460px; max-height: 85vh; overflow-y: auto; box-sizing: border-box; padding: 28px; border-radius: 20px; background: var(--cb-surface, #fff); color: var(--cb-ink, #17212b); box-shadow: 0 24px 60px rgba(15,23,42,.25); text-align: left; }
.privacy-title { display: block; font-size: 22px; font-weight: 700; line-height: 1.3; }
.privacy-copy { display: block; margin-top: 16px; font-size: 14px; line-height: 1.7; }
.privacy-links { display: flex; flex-wrap: wrap; gap: 16px; margin: 16px 0 24px; }
.privacy-link { padding: 4px 0; margin: 0; background: transparent; color: var(--cb-accent, #2368c4); font-size: 14px; line-height: 1.6; text-decoration: underline; }
.privacy-accept, .privacy-refuse { width: 100%; min-height: 48px; border-radius: 12px; font-size: 15px; font-weight: 600; line-height: 48px; }
.privacy-accept { background: var(--cb-accent, #2368c4); color: white; }
.privacy-refuse { margin-top: 8px; background: var(--cb-canvas, #f3f6fa); color: var(--cb-muted, #667483); }
button::after { border: 0; } button:hover { opacity: .85; } button:focus-visible { outline: 3px solid var(--cb-accent, #2368c4); outline-offset: 2px; }
.decline-copy { margin-bottom: 16px; }
@media (max-width: 560px) { .privacy-dialog { padding: 24px; } }
</style>
