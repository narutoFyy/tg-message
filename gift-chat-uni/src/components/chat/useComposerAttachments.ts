import { computed, ref } from 'vue'

export type ComposerAttachmentKind = 'image' | 'gif' | 'video'
export type ComposerAttachmentStatus = 'ready' | 'uploading' | 'failed'

export interface ComposerAttachment {
  id: string
  url: string
  kind: ComposerAttachmentKind
  name: string
  size: number
  status: ComposerAttachmentStatus
  error?: string
  revokeOnClear?: boolean
  file?: File
}

const MAX_IMAGE_ATTACHMENT_SIZE = 10 * 1024 * 1024
const MAX_VIDEO_ATTACHMENT_SIZE = 50 * 1024 * 1024

function nextAttachmentId() {
  return `attachment-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function inferKind(name = '', mimeType = ''): ComposerAttachmentKind {
  const lowerName = name.toLowerCase()
  const lowerType = mimeType.toLowerCase()
  if (lowerType.startsWith('video/') || /\.(mp4|webm|mov)$/.test(lowerName)) return 'video'
  return lowerType.includes('gif') || lowerName.endsWith('.gif') ? 'gif' : 'image'
}

function assertAllowedSize(size: number, kind: ComposerAttachmentKind) {
  const maxSize = kind === 'video' ? MAX_VIDEO_ATTACHMENT_SIZE : MAX_IMAGE_ATTACHMENT_SIZE
  if (size > maxSize) {
    throw new Error(kind === 'video' ? 'Video must be 50MB or smaller.' : 'Image or GIF must be 10MB or smaller.')
  }
}

export function useComposerAttachments() {
  const attachments = ref<ComposerAttachment[]>([])
  const activeAttachment = computed(() => attachments.value[0] || null)
  const hasAttachment = computed(() => Boolean(activeAttachment.value))
  const isUploading = computed(() => activeAttachment.value?.status === 'uploading')

  function clearAttachment(id?: string) {
    const removing = id
      ? attachments.value.filter((attachment) => attachment.id === id)
      : attachments.value
    removing.forEach((attachment) => {
      if (attachment.revokeOnClear) {
        URL.revokeObjectURL(attachment.url)
      }
    })
    attachments.value = id
      ? attachments.value.filter((attachment) => attachment.id !== id)
      : []
  }

  function setStatus(id: string, status: ComposerAttachmentStatus, error = '') {
    attachments.value = attachments.value.map((attachment) =>
      attachment.id === id ? { ...attachment, status, error } : attachment
    )
  }

  function addFile(file: File) {
    const kind = inferKind(file.name, file.type)
    assertAllowedSize(file.size, kind)
    clearAttachment()
    const attachment: ComposerAttachment = {
      id: nextAttachmentId(),
      url: URL.createObjectURL(file),
      kind,
      name: file.name || (kind === 'video' ? 'Video' : kind === 'gif' ? 'Pasted GIF' : 'Pasted image'),
      size: file.size,
      status: 'ready',
      revokeOnClear: true,
      file
    }
    attachments.value = [attachment]
    return attachment
  }

  function addPath(filePath: string, kind: ComposerAttachmentKind, name = '') {
    clearAttachment()
    const attachment: ComposerAttachment = {
      id: nextAttachmentId(),
      url: filePath,
      kind,
      name: name || (kind === 'video' ? 'Video' : kind === 'gif' ? 'GIF' : 'Image'),
      size: 0,
      status: 'ready',
      revokeOnClear: filePath.startsWith('blob:')
    }
    attachments.value = [attachment]
    return attachment
  }

  return {
    attachments,
    activeAttachment,
    hasAttachment,
    isUploading,
    addFile,
    addPath,
    clearAttachment,
    setStatus,
    inferKind,
    maxAttachmentSize: MAX_IMAGE_ATTACHMENT_SIZE,
    maxVideoAttachmentSize: MAX_VIDEO_ATTACHMENT_SIZE
  }
}
