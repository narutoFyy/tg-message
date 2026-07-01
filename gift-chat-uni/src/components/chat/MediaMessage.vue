<template>
  <view
    :class="['media-message', mediaType, { loading: isLoading, failed: hasError }]"
    :style="mediaStyle"
  >
    <view v-if="isLoading && !hasError" class="media-state">
      <text>Loading...</text>
    </view>

    <img
      v-if="!hasError"
      class="media-native-image"
      :src="resolvedSrc"
      alt=""
      @load="handleLoad"
      @error="handleError"
      @click="preview"
    />

    <view v-else class="media-error" @click="retryLoad">
      <text class="media-error-title">{{ unavailableText }}</text>
      <text class="media-error-copy">Tap to retry</text>
    </view>

    <view v-if="mediaType === 'gif'" class="media-badge">GIF</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const MAX_IMAGE_WIDTH = 280
const MAX_IMAGE_HEIGHT = 420
const MAX_GIF_WIDTH = 230
const MAX_MOBILE_IMAGE_WIDTH = 240
const MAX_MOBILE_IMAGE_HEIGHT = 360
const MAX_MOBILE_GIF_WIDTH = 220
const MIN_MEDIA_WIDTH = 120
const MIN_MEDIA_HEIGHT = 80

const props = defineProps<{
  src: string
  mediaType: 'image' | 'gif'
}>()

const emit = defineEmits<{
  (event: 'preview', url: string): void
}>()

const isLoading = ref(true)
const hasError = ref(false)
const retryToken = ref(0)
const naturalWidth = ref(0)
const naturalHeight = ref(0)

const resolvedSrc = computed(() => {
  const url = resolveMediaUrl(props.src)
  if (!url || retryToken.value === 0) return url
  const separator = url.includes('?') ? '&' : '?'
  return `${url}${separator}retry=${retryToken.value}`
})
const unavailableText = computed(() => props.mediaType === 'gif' ? 'GIF unavailable' : 'Image unavailable')
const mediaStyle = computed(() => {
  if (hasError.value || !naturalWidth.value || !naturalHeight.value) {
    return {}
  }

  const size = fitMediaSize(naturalWidth.value, naturalHeight.value, props.mediaType)
  return {
    width: `${size.width}px`,
    height: `${size.height}px`,
    aspectRatio: `${naturalWidth.value} / ${naturalHeight.value}`
  }
})

watch(() => props.src, (src) => {
  const hasSource = !!resolveMediaUrl(src)
  isLoading.value = hasSource
  hasError.value = !hasSource
  retryToken.value = 0
  naturalWidth.value = 0
  naturalHeight.value = 0
}, { immediate: true })

function handleLoad(event: Event) {
  const image = event.target as HTMLImageElement | null
  naturalWidth.value = image?.naturalWidth || 0
  naturalHeight.value = image?.naturalHeight || 0
  isLoading.value = false
  hasError.value = false
}

function handleError() {
  isLoading.value = false
  hasError.value = true
}

function retryLoad() {
  isLoading.value = true
  hasError.value = false
  retryToken.value += 1
}

function preview() {
  if (!hasError.value && resolvedSrc.value) {
    emit('preview', resolvedSrc.value)
  }
}

function fitMediaSize(width: number, height: number, type: 'image' | 'gif') {
  const maxWidth = getResponsiveMaxWidth(type)
  const maxHeight = getResponsiveMaxHeight()
  const scale = Math.min(maxWidth / width, maxHeight / height, 1)
  const fittedWidth = Math.max(MIN_MEDIA_WIDTH, Math.round(width * scale))
  const fittedHeight = Math.max(MIN_MEDIA_HEIGHT, Math.round(height * scale))

  return {
    width: fittedWidth,
    height: fittedHeight
  }
}

function getResponsiveMaxWidth(type: 'image' | 'gif') {
  if (typeof window !== 'undefined' && window.innerWidth <= 768) {
    return type === 'gif' ? MAX_MOBILE_GIF_WIDTH : MAX_MOBILE_IMAGE_WIDTH
  }
  return type === 'gif' ? MAX_GIF_WIDTH : MAX_IMAGE_WIDTH
}

function getResponsiveMaxHeight() {
  if (typeof window !== 'undefined' && window.innerWidth <= 768) {
    return MAX_MOBILE_IMAGE_HEIGHT
  }
  return MAX_IMAGE_HEIGHT
}
</script>

<style scoped>
.media-message {
  position: relative;
  width: auto;
  min-height: 96px;
  max-width: min(280px, 54vw);
  max-height: 420px;
  overflow: hidden;
  border-radius: 8px;
  background: #f6faf4;
  box-shadow: inset 0 0 0 1px rgba(42, 68, 43, 0.08);
  cursor: pointer;
}

.media-message.gif {
  max-width: min(230px, 50vw);
}

.media-native-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f6faf4;
}

.media-message.loading .media-native-image {
  opacity: 0;
}

.media-message.gif .media-native-image {
  max-width: 100%;
}

.media-state,
.media-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 112px;
  padding: 16px;
  box-sizing: border-box;
  color: #6f8069;
  font-size: 13px;
  font-weight: 800;
  text-align: center;
}

.media-state {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(180deg, #f7fbf5, #eef7e9);
}

.media-error {
  flex-direction: column;
  gap: 5px;
  border: 1px dashed rgba(190, 66, 45, 0.35);
  background: #fff7f5;
  color: #b42318;
}

.media-error-title,
.media-error-copy {
  display: block;
  line-height: 1.3;
}

.media-error-copy {
  color: #7a8a75;
  font-size: 11px;
  font-weight: 700;
}

.media-badge {
  position: absolute;
  right: 8px;
  bottom: 8px;
  min-width: 34px;
  height: 22px;
  padding: 0 7px;
  border-radius: 6px;
  background: rgba(17, 24, 39, 0.76);
  color: #ffffff;
  font-size: 11px;
  font-weight: 900;
  line-height: 22px;
  text-align: center;
  box-sizing: border-box;
}

@media (max-width: 768px) {
  .media-message {
    max-width: min(240px, 66vw);
    max-height: 360px;
  }

  .media-message.gif {
    max-width: min(220px, 62vw);
  }
}
</style>
