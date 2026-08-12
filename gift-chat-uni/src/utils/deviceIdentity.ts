export type ClientDeviceType = 'mobile' | 'desktop'

export interface ClientDeviceIdentity {
  deviceId: string
  deviceType: ClientDeviceType
}

const DEVICE_ID_KEY = 'xcard-client-device-id'

export function getClientDeviceIdentity(): ClientDeviceIdentity {
  let deviceId = String(uni.getStorageSync(DEVICE_ID_KEY) || '').trim()
  if (!deviceId) {
    deviceId = createDeviceId()
    uni.setStorageSync(DEVICE_ID_KEY, deviceId)
  }
  return { deviceId, deviceType: detectDeviceType() }
}

function createDeviceId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `device-${Date.now()}-${Math.random().toString(36).slice(2, 14)}`
}

function detectDeviceType(): ClientDeviceType {
  try {
    const system = uni.getSystemInfoSync()
    const deviceType = String(system.deviceType || '').toLowerCase()
    const platform = String(system.platform || '').toLowerCase()
    if (deviceType === 'phone' || deviceType === 'pad' || ['ios', 'android'].includes(platform)) {
      return 'mobile'
    }
  } catch {
    // Fall through to browser user-agent detection.
  }
  if (typeof navigator !== 'undefined' && /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent)) {
    return 'mobile'
  }
  return 'desktop'
}
