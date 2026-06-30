<template>
  <view :class="['media-message', mediaType]">
    <image
      class="media-image"
      :src="src"
      mode="aspectFill"
      @click="$emit('preview', src)"
    />
    <view v-if="mediaType === 'gif'" class="media-badge">GIF</view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  src: string
  mediaType: 'image' | 'gif'
}>()

defineEmits<{
  (event: 'preview', url: string): void
}>()
</script>

<style scoped>
.media-message {
  position: relative;
  width: min(240px, 58vw);
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 8px;
  background: #eef2ec;
  box-shadow: inset 0 0 0 1px rgba(42, 68, 43, 0.08);
}

.media-message.gif {
  aspect-ratio: 1 / 1;
}

.media-image {
  width: 100%;
  height: 100%;
  display: block;
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
</style>
