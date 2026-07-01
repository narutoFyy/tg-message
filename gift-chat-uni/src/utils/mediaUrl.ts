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
    if (uploadPath && shouldUseSameOriginUpload(absoluteUrl)) {
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

function shouldUseSameOriginUpload(url: URL) {
  if (typeof window === 'undefined') {
    return false
  }

  return url.origin === window.location.origin
}
