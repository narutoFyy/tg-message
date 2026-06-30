<template>
  <view :class="['chat-message', mine ? 'mine' : 'theirs', message.author === 'system' ? 'system' : '']">
    <image v-if="!mine && message.author !== 'system'" class="chat-avatar" :src="avatarSrc" mode="aspectFit" />

    <view :class="['chat-bubble', message.author === 'system' ? 'system-bubble' : '']">
      <MediaMessage
        v-if="message.type === 'image' || message.type === 'gif'"
        :src="message.content"
        :media-type="message.type"
        @preview="$emit('preview', $event)"
      />
      <view v-else-if="message.type === 'voice'" class="voice-chip" @click="$emit('playVoice', message.content)">
        <text>{{ voiceLabel }}</text>
      </view>
      <CallMessageCard
        v-else-if="message.type === 'video'"
        :title="callTitle || defaultCallTitle"
        :room="callRoom || ''"
        :status="callStatus || 'created'"
        :status-label="callStatusLabel || ''"
        :caption="callCaption"
        :answer-label="callAnswerLabel"
        :reject-label="callRejectLabel"
        :enter-label="callEnterLabel"
        :can-answer="canAnswerCall"
        :can-reject="canRejectCall"
        :can-enter="canEnterCall"
        @answer="$emit('answerCall', message)"
        @reject="$emit('rejectCall', message)"
        @enter="$emit('enterCall', message)"
      />
      <text v-else class="message-text">{{ message.content }}</text>

      <text v-if="translation" class="translation-text">{{ translation }}</text>

      <view v-if="message.author !== 'system'" class="message-meta">
        <text class="msg-time">{{ message.createdAt }}</text>
        <MessageDeliveryStatus
          v-if="mine"
          :state="message.readState"
          @retry="$emit('retry', message)"
        />
      </view>
    </view>

    <image v-if="mine" class="chat-avatar" :src="avatarSrc" mode="aspectFit" />
  </view>
</template>

<script setup lang="ts">
import CallMessageCard from './CallMessageCard.vue'
import MediaMessage from './MediaMessage.vue'
import MessageDeliveryStatus from './MessageDeliveryStatus.vue'
import type { ChatMessage, VideoSessionItem } from '@/types'

withDefaults(defineProps<{
  message: ChatMessage
  mine: boolean
  avatarSrc: string
  translation?: string
  voiceLabel?: string
  callTitle?: string
  callRoom?: string
  callStatus?: VideoSessionItem['status'] | string
  callStatusLabel?: string
  callCaption?: string
  callAnswerLabel?: string
  callRejectLabel?: string
  callEnterLabel?: string
  canAnswerCall?: boolean
  canRejectCall?: boolean
  canEnterCall?: boolean
}>(), {
  translation: '',
  voiceLabel: 'Voice message',
  callTitle: '',
  callRoom: '',
  callStatus: 'created',
  callStatusLabel: '',
  callCaption: '',
  callAnswerLabel: 'Answer',
  callRejectLabel: 'Decline',
  callEnterLabel: 'Enter',
  canAnswerCall: false,
  canRejectCall: false,
  canEnterCall: false
})

defineEmits<{
  (event: 'preview', url: string): void
  (event: 'playVoice', url: string): void
  (event: 'retry', message: ChatMessage): void
  (event: 'answerCall', message: ChatMessage): void
  (event: 'rejectCall', message: ChatMessage): void
  (event: 'enterCall', message: ChatMessage): void
}>()

const defaultCallTitle = 'Video call'
</script>

<style scoped>
.chat-message {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  min-width: 0;
}

.chat-message.mine {
  justify-content: flex-end;
}

.chat-message.system {
  justify-content: center;
}

.chat-avatar {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: 50%;
  background: #e0e0e0;
}

.chat-bubble {
  max-width: min(68%, 520px);
  min-width: 0;
  padding: 10px 14px;
  border-radius: 8px;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(42, 68, 43, 0.08);
  box-sizing: border-box;
}

.chat-message.theirs .chat-bubble {
  background: rgba(255, 255, 255, 0.92);
  border-top-left-radius: 2px;
}

.chat-message.mine .chat-bubble {
  background: linear-gradient(180deg, #efffd1, #f5ffd9);
  border-top-right-radius: 2px;
}

.system-bubble {
  max-width: 80%;
  margin: 0 auto;
  background: rgba(0, 0, 0, 0.1) !important;
  text-align: center;
  font-size: 12px;
  color: #777777;
  border-radius: 4px !important;
  box-shadow: none;
}

.message-text {
  color: #333333;
  font-size: 15px;
  line-height: 1.5;
}

.voice-chip {
  min-width: 140px;
  color: #243329;
  font-size: 14px;
  font-weight: 800;
}

.translation-text {
  display: block;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  color: #20805f;
  font-size: 13px;
  line-height: 1.45;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 5px;
}

.msg-time {
  color: #999999;
  font-size: 11px;
}

@media (max-width: 768px) {
  .chat-bubble {
    max-width: min(78%, 420px);
  }

  .chat-avatar {
    width: 32px;
    height: 32px;
    flex-basis: 32px;
  }
}
</style>
