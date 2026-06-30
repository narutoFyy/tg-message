<template>
  <view :class="['media-message', mediaType]">
    <img
      class="media-native-image"
      :src="resolvedSrc"
      alt=""
      @click="$emit('preview', resolvedSrc)"
    />
    <view v-if="mediaType === 'gif'" class="media-badge">GIF</view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const props = defineProps<{
  src: string
  mediaType: 'image' | 'gif'
}>()

defineEmits<{
  (event: 'preview', url: string): void
}>()

const resolvedSrc = computed(() => resolveMediaUrl(props.src))
</script>

<style scoped>
.media-message {
  position: relative;
  width: min(300px, 58vw);
  max-height: 420px;
  overflow: hidden;
  border-radius: 8px;
  background: #f6faf4;
  box-shadow: inset 0 0 0 1px rgba(42, 68, 43, 0.08);
  cursor: pointer;
}

.media-message.gif {
  width: min(240px, 54vw);
}

.media-native-image {
  display: block;
  width: 100%;
  max-width: 100%;
  height: auto;
  max-height: 420px;
  object-fit: contain;
  background: #f6faf4;
}

.media-message.gif .media-native-image {
  max-width: 100%;
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
    width: min(270px, 68vw);
    max-height: 360px;
  }

  .media-message.gif {
    width: min(230px, 62vw);
  }

  .media-native-image {
    max-height: 360px;
  }
}
</style>
