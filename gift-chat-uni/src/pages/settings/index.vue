<template>
  <view class="page-shell soft-page settings-page">
    <view class="page-stack">
      <view class="page-header settings-hero tone-social">
        <view class="page-header-copy">
          <text class="eyebrow">Preferences</text>
          <text class="title">Settings</text>
          <text class="subtitle">Account details and support tools.</text>
        </view>
      </view>

      <view class="settings-group account-group">
        <view class="group-head"><text class="section-title">Account</text></view>
        <view class="info-row"><text class="info-label">Username</text><text class="value-copy">{{ store.state.currentUser?.username || 'Not signed in' }}</text></view>
        <view class="info-row"><text class="info-label">Email</text><text class="value-copy">{{ store.state.currentUser?.email || 'Not set' }}</text></view>
        <view class="info-row"><text class="info-label">Phone</text><text class="value-copy">{{ store.state.currentUser?.phone || 'Not set' }}</text></view>
        <view class="info-row"><text class="info-label">Invited by</text><text class="value-copy">{{ invitedBy }}</text></view>
        <view class="info-row"><text class="info-label">Role</text><text class="value-copy">{{ store.state.currentUser?.roleCode || 'Not signed in' }}</text></view>
      </view>

      <view v-if="store.state.currentUser?.roleCode === 'USER'" class="settings-group notifications-group">
        <view class="group-head"><text class="section-title">Notifications</text></view>
        <view class="notification-row">
          <view class="notification-copy">
            <text class="notification-title">Support messages</text>
            <text class="notification-status">{{ notificationStatus }}</text>
          </view>
          <switch
            :key="pushSwitchKey"
            color="#0f7f49"
            :checked="pushState.subscribed"
            :disabled="pushBusy || !pushState.supported || !pushState.serverEnabled"
            @change="togglePushNotifications"
          />
        </view>
      </view>

      <view class="settings-group action-group">
        <button class="action-row legal-action" @click="openLegal('privacy')">Privacy Policy</button>
        <button class="action-row legal-action" @click="openLegal('terms')">Terms of Use</button>
        <view class="action-row" @click="copyInvite"><text>Copy invite code</text><text class="action-value">{{ inviteCode }}</text></view>
        <view class="action-row" @click="goSupport"><text>Support</text><text class="action-value">Open chat</text></view>
        <view v-if="store.state.currentUser?.roleCode === 'ADMIN'" class="action-row" @click="goAdminRates"><text>Rate admin</text><text class="action-value">Open</text></view>
        <view v-if="store.state.currentUser?.roleCode === 'ADMIN'" class="action-row" @click="goAdminConsole"><text>Admin console</text><text class="action-value">Open</text></view>
      </view>

      <button class="logout-button" @click="logout">Log out</button>
      <view v-if="store.state.currentUser?.roleCode === 'USER'" class="delete-section">
        <text class="delete-title">Delete account</text>
        <text class="delete-copy">Permanently close your account and remove identifying profile details. Transaction records and conversation history may be retained. Contact Support about pending orders or balances first.</text>
        <button class="delete-button" @click="openDeletion">Delete account</button>
      </view>
      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>
    <view v-if="deletionOpen" class="delete-overlay" @touchmove.stop.prevent>
      <view class="delete-dialog" role="dialog" aria-modal="true" aria-label="Delete your account">
        <text class="delete-title">Delete your account?</text>
        <text class="delete-copy">This is permanent. You will be signed out and lose account access. Profile and saved bank details will be removed; messages, uploaded files and transaction records may be retained as explained in the Privacy Policy.</text>
        <text class="delete-label">Confirm your password</text>
        <input v-model="deletePassword" class="delete-input" password :disabled="deleteBusy" placeholder="Enter your password" />
        <text v-if="deleteError" class="delete-error" role="alert">{{ deleteError }}</text>
        <button class="delete-button" :disabled="deleteBusy || !deletePassword" :loading="deleteBusy" @click="confirmDeletion">{{ deleteBusy ? 'Deleting account…' : 'Permanently delete account' }}</button>
        <button class="delete-cancel" :disabled="deleteBusy" @click="closeDeletion">Keep account</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { openLegal } from '@/utils/privacy'
import {
  disableWebPushNotifications,
  enableWebPushNotifications,
  readWebPushState,
  type WebPushState
} from '@/utils/webPush'

const store = useAppStore()
const notice = ref('')
const deletionOpen = ref(false)
const deletePassword = ref('')
const deleteError = ref('')
const deleteBusy = ref(false)

function openDeletion() { deletePassword.value = ''; deleteError.value = ''; deletionOpen.value = true }
function closeDeletion() { if (!deleteBusy.value) { deletionOpen.value = false; deletePassword.value = '' } }
async function confirmDeletion() {
  if (deleteBusy.value || !deletePassword.value) return
  deleteBusy.value = true
  deleteError.value = ''
  try {
    await store.deleteAccount(deletePassword.value)
    deletePassword.value = ''
    deletionOpen.value = false
    // The server deletes push bindings; browser unsubscription is best effort.
    // #ifdef H5
    try {
      const registration = await navigator.serviceWorker?.getRegistration('/')
      await (await registration?.pushManager?.getSubscription())?.unsubscribe()
    } catch { /* Account deletion has already succeeded. */ }
    // #endif
    uni.reLaunch({ url: '/pages/login/index' })
    uni.showToast({ title: 'Account deleted.', icon: 'none' })
  } catch (error) {
    deleteError.value = error instanceof Error ? error.message : 'Deletion failed. Please try again.'
  } finally { deleteBusy.value = false }
}
const inviteCode = computed(() => store.state.currentUser?.inviteCode || 'Unavailable')
const invitedBy = computed(() => store.state.currentUser?.invitedBy || 'Direct registration')
const pushBusy = ref(false)
const pushSwitchKey = ref(0)
const pushState = ref<WebPushState>({
  supported: false,
  serverEnabled: false,
  subscribed: false,
  permission: 'unsupported',
  requiresHomeScreen: false
})

const notificationStatus = computed(() => {
  if (pushBusy.value) return 'Updating...'
  if (pushState.value.requiresHomeScreen) return 'Open Xcard from the Home Screen to enable.'
  if (!pushState.value.supported) return 'Not supported by this browser.'
  if (!pushState.value.serverEnabled) return 'Not available yet.'
  if (pushState.value.permission === 'denied') return 'Blocked in browser settings.'
  return pushState.value.subscribed ? 'On' : 'Off'
})

onShow(() => {
  if (store.state.currentUser?.roleCode === 'USER') refreshPushState()
})

async function refreshPushState() {
  try {
    pushState.value = await readWebPushState()
  } catch {
    pushState.value = { ...pushState.value, serverEnabled: false }
  }
}

async function togglePushNotifications(event: Event) {
  if (pushBusy.value) return
  const enabled = (event as Event & { detail: { value: boolean } }).detail.value
  pushBusy.value = true
  notice.value = ''
  try {
    pushState.value = enabled
      ? await enableWebPushNotifications()
      : await disableWebPushNotifications()
    notice.value = enabled ? 'Message notifications enabled.' : 'Message notifications disabled.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Notification settings could not be updated.'
    await refreshPushState()
    pushSwitchKey.value += 1
  } finally {
    pushBusy.value = false
  }
}

function copyInvite() {
  if (!store.state.currentUser?.inviteCode) { notice.value = 'Invite code is not available.'; return }
  uni.setClipboardData({ data: `Invite code: ${inviteCode.value}`, success() { notice.value = 'Invite code copied.' } })
}
function goSupport() { uni.redirectTo({ url: '/pages/support/index' }) }
function goAdminRates() { uni.navigateTo({ url: '/pages/admin-rates/index' }) }
function goAdminConsole() { uni.navigateTo({ url: '/pages/admin-console/index' }) }
async function logout() {
  if (store.state.currentUser?.roleCode === 'USER') {
    try { await disableWebPushNotifications() } catch { /* The local subscription is still removed. */ }
  }
  await store.logout()
  uni.redirectTo({ url: '/pages/login/index' })
}
</script>

<style scoped lang="scss">
.legal-action { width: 100%; margin: 0; background: transparent; text-align: left; border-radius: 0; line-height: 1.5; }
.legal-action::after { border: 0; }
.delete-section { padding: 24px; border-radius: 16px; background: var(--cb-surface); }
.delete-title { display: block; font-size: 20px; font-weight: 700; color: var(--cb-ink); line-height: 1.4; }
.delete-copy { display: block; margin-top: 12px; font-size: 14px; color: var(--cb-muted); line-height: 1.7; }
.delete-button { width: 100%; min-height: 48px; margin-top: 20px; padding: 8px 16px; border: 1px solid #fecaca; border-radius: 10px; background: var(--cb-surface); color: #b91c1c; font-size: 14px; line-height: 1.6; font-weight: 600; }
.delete-button::after, .delete-cancel::after { border: 0; }
.delete-button:hover:not(:disabled) { background: #fef2f2; }
.delete-button:disabled { opacity: .55; }
.delete-button:focus-visible, .delete-cancel:focus-visible, .legal-action:focus-visible { outline: 3px solid var(--cb-accent); outline-offset: 2px; }
.delete-overlay { position: fixed; inset: 0; z-index: 9999; display: flex; align-items: center; justify-content: center; padding: 24px; box-sizing: border-box; background: rgba(15,23,42,.55); }
.delete-dialog { width: 100%; max-width: 460px; max-height: 85vh; overflow-y: auto; padding: 28px; box-sizing: border-box; border-radius: 20px; background: var(--cb-surface); }
.delete-label { display: block; margin: 20px 0 8px; font-size: 14px; font-weight: 600; }
.delete-input { box-sizing: border-box; height: 48px; padding: 0 14px; border: 1px solid var(--cb-line); border-radius: 12px; font-size: 14px; color: var(--cb-ink); }
.delete-input:focus-within { border-color: var(--cb-accent); }
.delete-error { display: block; margin-top: 12px; font-size: 14px; color: #b91c1c; }
.delete-cancel { width: 100%; margin-top: 8px; min-height: 48px; background: var(--cb-canvas); color: var(--cb-ink); border-radius: 10px; font-size: 14px; line-height: 48px; }
.delete-cancel:hover:not(:disabled), .legal-action:hover { background: var(--cb-canvas); }
.settings-page { padding-bottom: 48rpx; }
.page-header-copy .eyebrow, .page-header-copy .title, .page-header-copy .subtitle { display: block; }
.page-header-copy .title { margin-top: 7rpx; }
.page-header-copy .subtitle { margin-top: 8rpx; }
.settings-hero { padding: 28rpx; align-items: center; border-bottom: 0; }
.settings-group { background: #ffffff; border: 1rpx solid var(--cb-line); border-radius: 12rpx; overflow: hidden; }
.account-group { background: var(--cb-sky); border-color: #cfe4fb; }
.action-group { background: var(--cb-mint); border-color: #c6ead8; }
.notifications-group { background: var(--cb-lilac); border-color: #ddd5fb; }
.group-head { min-height: 72rpx; padding: 0 24rpx; display: flex; align-items: center; border-bottom: 1rpx solid #dedfe3; }
.info-row, .action-row { min-height: 76rpx; padding: 15rpx 24rpx; box-sizing: border-box; display: flex; align-items: center; justify-content: space-between; gap: 22rpx; border-bottom: 1rpx solid #dedfe3; }
.info-row:last-child, .action-row:last-child { border-bottom: 0; }
.info-label { flex: 0 0 auto; color: #6f7178; font-size: 24rpx; }
.value-copy { min-width: 0; color: #111111; font-size: 25rpx; font-weight: 700; text-align: right; overflow-wrap: anywhere; }
.action-row { color: #111111; font-size: 25rpx; font-weight: 700; }
.action-row:active { border-left: 5rpx solid var(--cb-mint-strong); }
.action-value { max-width: 50%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #0f7f49; font-size: 22rpx; }
.notification-row { min-height: 94rpx; padding: 15rpx 24rpx; box-sizing: border-box; display: flex; align-items: center; justify-content: space-between; gap: 22rpx; }
.notification-copy { min-width: 0; flex: 1; }
.notification-title, .notification-status { display: block; }
.notification-title { color: #111111; font-size: 25rpx; font-weight: 700; }
.notification-status { margin-top: 5rpx; color: #6f7178; font-size: 21rpx; line-height: 1.35; }
.logout-button { width: 100%; min-height: 82rpx; border: 1rpx solid #dedfe3; border-radius: 5rpx; background: #ffffff; color: #b42318; font-size: 26rpx; font-weight: 700; }
.logout-button::after { border: 0; }
.notice-text { display: block; color: #6f7178; font-size: 23rpx; text-align: center; }
</style>
