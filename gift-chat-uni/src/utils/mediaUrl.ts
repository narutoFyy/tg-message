const UPLOAD_PATH_PATTERN = /\/uploads\/(images|voices)\//

export function resolveMediaUrl(url: string) {
  const value = (url || '').trim()
  if (!value) {
    return ''
  }

  if (value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }

  const absoluteUrl = parseAbsoluteUrl(value)
  if (absoluteUrl) {
    const uploadPath = extractUploadPath(`${absoluteUrl.pathname}${absoluteUrl.search}${absoluteUrl.hash}`)
    if (uploadPath && shouldUseSameOriginUpload(absoluteUrl.hostname)) {
      return uploadPath
    }
    return value
  }

  const uploadPath = extractUploadPath(value)
  if (uploadPath) {
    return uploadPath
  }

  return value.startsWith('/') ? value : `/${value}`
}

function extractUploadPath(value: string) {
  const match = value.match(UPLOAD_PATH_PATTERN)
  if (!match || match.index === undefined) {
    return ''
  }
  return value.slice(match.index)
}

function parseAbsoluteUrl(value: string) {
  if (!/^https?:\/\//i.test(value) || typeof URL === 'undefined') {
    return null
  }

  try {
    return new URL(value)
  } catch {
    return null
  }
}

function shouldUseSameOriginUpload(hostname: string) {
  const normalized = hostname.toLowerCase()
  const currentHostname = getCurrentHostname()
  return (
    (!!currentHostname && normalized === currentHostname) ||
    normalized === 'localhost' ||
    normalized === '127.0.0.1' ||
    normalized === '0.0.0.0' ||
    normalized === '::1' ||
    normalized.startsWith('10.') ||
    normalized.startsWith('192.168.') ||
    /^172\.(1[6-9]|2\d|3[0-1])\./.test(normalized)
  )
}

function getCurrentHostname() {
  if (typeof window === 'undefined') {
    return ''
  }

  return window.location.hostname.toLowerCase()
}
