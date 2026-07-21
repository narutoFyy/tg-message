<template>
  <view :class="['chat-message', mine ? 'mine' : 'theirs', message.author === 'system' ? 'system' : '']">
    <image v-if="!mine && message.author !== 'system'" class="chat-avatar" :src="avatarSrc" mode="aspectFit" />

    <view
      :class="['chat-bubble', bubbleKindClass, message.author === 'system' ? 'system-bubble' : '']"
      @contextmenu.prevent.stop="emitMessageMenu"
      @longpress="emitMessageMenu"
    >
      <view v-if="message.replyTo" class="reply-preview">
        <text class="reply-preview-line">{{ replyPreviewText }}</text>
      </view>
      <OrderMessageCard
        v-if="message.type === 'order' && message.order"
        :order="message.order"
        :can-manage="canManageOrder"
        @preview="$emit('preview', $event)"
        @complete="$emit('completeOrder', $event)"
        @cancel="$emit('cancelOrder', $event)"
      />
      <MediaMessage
        v-else-if="isImageAttachment"
        :src="mediaSource"
        :media-type="imageMediaType"
        @preview="$emit('preview', $event)"
      />
      <view v-else-if="isVideoAttachment" class="video-message">
        <video class="media-native-video" :src="mediaSource" controls />
      </view>
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

      <text v-if="mediaCaption" class="media-caption">{{ mediaCaption }}</text>

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
import { computed } from 'vue'
import CallMessageCard from './CallMessageCard.vue'
import OrderMessageCard from './OrderMessageCard.vue'
import MediaMessage from './MediaMessage.vue'
import MessageDeliveryStatus from './MessageDeliveryStatus.vue'
import type { ChatMessage, VideoSessionItem } from '@/types'
import { resolveMediaUrl } from '@/utils/mediaUrl'

const props = withDefaults(defineProps<{
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
  canManageOrder?: boolean
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
  canEnterCall: false,
  canManageOrder: false
})

const emit = defineEmits<{
  (event: 'preview', url: string): void
  (event: 'playVoice', url: string): void
  (event: 'retry', message: ChatMessage): void
  (event: 'answerCall', message: ChatMessage): void
  (event: 'rejectCall', message: ChatMessage): void
  (event: 'enterCall', message: ChatMessage): void
  (event: 'messageMenu', message: ChatMessage, point?: { clientX: number; clientY: number }): void
  (event: 'completeOrder', order: NonNullable<ChatMessage['order']>): void
  (event: 'cancelOrder', order: NonNullable<ChatMessage['order']>): void
}>()

const defaultCallTitle = 'Video call'
const mediaAttachment = computed(() => props.message.attachments?.find((attachment) =>
  attachment.type === 'image' || attachment.type === 'gif' || attachment.type === 'video'
) || null)
const isImageAttachment = computed(() => props.message.type === 'image' || props.message.type === 'gif')
const imageMediaType = computed<'image' | 'gif'>(() => props.message.type === 'gif' ? 'gif' : 'image')
const isVideoAttachment = computed(() => props.message.type === 'video' && mediaAttachment.value?.type === 'video')
const mediaSource = computed(() => resolveMediaUrl(mediaAttachment.value?.url || props.message.content))
const mediaCaption = computed(() => {
  if (!isImageAttachment.value && !isVideoAttachment.value) return ''
  const content = props.message.content.trim()
  const attachmentUrl = mediaAttachment.value?.url?.trim() || ''
  return content && content !== attachmentUrl ? content : ''
})
const bubbleKindClass = computed(() => {
  if (isImageAttachment.value || isVideoAttachment.value) return 'media-bubble'
  if (props.message.type === 'video') return 'call-bubble'
  if (props.message.type === 'order') return 'order-bubble'
  return ''
})

const replyPreviewText = computed(() => {
  const content = props.message.replyTo?.content?.trim() || ''
  if (!content) return ''
  return content.length > 80 ? `${content.slice(0, 80)}...` : content
})

function eventPoint(event: Event) {
  const raw = event as MouseEvent & {
    touches?: ArrayLike<Touch>
    changedTouches?: ArrayLike<Touch>
    detail?: { x?: number; y?: number; clientX?: number; clientY?: number }
  }
  const touch = raw.changedTouches?.[0] || raw.touches?.[0]
  return {
    clientX: raw.clientX || touch?.clientX || raw.detail?.clientX || raw.detail?.x || 0,
    clientY: raw.clientY || touch?.clientY || raw.detail?.clientY || raw.detail?.y || 0
  }
}

function emitMessageMenu(event: Event) {
  if (props.message.author === 'system') return
  emit('messageMenu', props.message, eventPoint(event))
}
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
  min-width: 118px;
  padding: 10px 14px;
  border-radius: 8px;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(42, 68, 43, 0.08);
  box-sizing: border-box;
}

.chat-bubble.media-bubble {
  width: auto;
  max-width: none;
  flex: 0 0 auto;
  min-width: 0;
  overflow: hidden;
  padding: 0;
  background: #f6faf4 !important;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(42, 68, 43, 0.08);
}

.chat-bubble.call-bubble {
  min-width: 0;
  padding: 0;
  background: transparent !important;
  box-shadow: none;
}

.chat-message.theirs .chat-bubble {
  background: rgba(255, 255, 255, 0.92);
  border-top-left-radius: 2px;
}

.chat-message.mine .chat-bubble {
  background: #e8f6ef;
  border-top-right-radius: 2px;
}

.chat-message.theirs .chat-bubble.media-bubble,
.chat-message.mine .chat-bubble.media-bubble,
.chat-message.theirs .chat-bubble.call-bubble,
.chat-message.mine .chat-bubble.call-bubble {
  border-radius: 8px;
}

.system-bubble {
  max-width: min(78%, 460px);
  min-width: 0;
  margin: 0 auto;
  padding: 5px 10px;
  background: rgba(23, 33, 43, 0.1) !important;
  text-align: center;
  border-radius: 999px !important;
  box-shadow: none;
}

.system-bubble .message-text {
  color: #66737f;
  font-size: 12px;
  line-height: 1.35;
}

.message-text {
  color: #333333;
  font-size: 15px;
  line-height: 1.5;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  user-select: text;
  -webkit-user-select: text;
}

.reply-preview {
  margin: 0 0 8px;
  padding: 6px 8px;
  border-left: 3px solid rgba(32, 128, 95, 0.36);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.58);
  box-sizing: border-box;
}

.reply-preview-line {
  display: block;
  color: #4d6558;
  font-size: 13px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  min-width: 0;
  white-space: nowrap;
  flex-wrap: nowrap;
}

.media-bubble .message-meta {
  margin-top: 0;
  padding: 5px 8px 6px;
  background: rgba(255, 255, 255, 0.86);
  border-radius: 0 0 8px 8px;
}

.chat-bubble.order-bubble {
  min-width: 0;
  max-width: none;
  padding: 0;
  background: transparent !important;
  box-shadow: none;
}

.video-message {
  width: min(520px, 72vw);
  max-width: 100%;
  background: #111111;
}

.media-native-video {
  display: block;
  width: 100%;
  min-height: 220px;
  max-height: 520px;
  background: #111111;
}

.media-caption {
  display: block;
  max-width: min(520px, 72vw);
  padding: 9px 11px 2px;
  color: #28332d;
  font-size: 14px;
  line-height: 1.45;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.msg-time {
  flex: 0 0 auto;
  color: #999999;
  font-size: 11px;
}

@media (max-width: 768px) {
  .chat-bubble {
    max-width: min(78%, 420px);
  }

  .chat-bubble.media-bubble {
    width: auto;
    max-width: none;
  }

  .chat-avatar {
    width: 32px;
    height: 32px;
    flex-basis: 32px;
  }
}
</style>
