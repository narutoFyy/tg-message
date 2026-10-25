// Android Photo Picker grants access only to the selected item. Older devices
// use ACTION_OPEN_DOCUMENT, which also needs no library/storage permission.
// Keep the native bridge here so all avatar, trade and chat upload paths agree.
type NativeObject = any
declare const plus: {
  os: { name: string }
  android: {
    runtimeMainActivity(): NativeObject
    importClass(name: string): NativeObject
    newObject(name: string, ...args: NativeObject[]): NativeObject
    invoke(object: NativeObject, method: string, ...args: NativeObject[]): NativeObject
  }
} | undefined

let pickerBusy = false
let nextRequestCode = 48000

function isAndroidApp() {
  // #ifdef APP-PLUS
  return typeof plus !== 'undefined' && plus.os.name === 'Android'
  // #endif
  // #ifndef APP-PLUS
  return false
  // #endif
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error)
}

function pickAndroidMedia(kind: 'image' | 'video'): Promise<string> {
  if (pickerBusy) return Promise.reject(new Error('A media picker is already open.'))
  pickerBusy = true
  return new Promise((resolve, reject) => {
    const android = plus!.android
    let activity: NativeObject
    let previousResult: NativeObject
    let resultHandler: NativeObject
    const requestCode = nextRequestCode++
    if (nextRequestCode > 49000) nextRequestCode = 48000

    const cleanup = () => {
      if (activity && activity.onActivityResult === resultHandler) {
        activity.onActivityResult = previousResult
      }
      pickerBusy = false
    }
    try {
      activity = android.runtimeMainActivity()
      previousResult = activity.onActivityResult
      const sdk = Number(android.importClass('android.os.Build$VERSION').SDK_INT)
      const action = sdk >= 33 ? 'android.provider.action.PICK_IMAGES' : 'android.intent.action.OPEN_DOCUMENT'
      let intent = android.newObject('android.content.Intent', action)
      android.invoke(intent, 'setType', `${kind}/*`)
      const packageManager = android.invoke(activity, 'getPackageManager')
      if (sdk >= 33 && !android.invoke(intent, 'resolveActivity', packageManager)) {
        intent = android.newObject('android.content.Intent', 'android.intent.action.OPEN_DOCUMENT')
        android.invoke(intent, 'setType', `${kind}/*`)
      }
      if (String(android.invoke(intent, 'getAction')) === 'android.intent.action.OPEN_DOCUMENT') {
        android.invoke(intent, 'addCategory', 'android.intent.category.OPENABLE')
        android.invoke(intent, 'addFlags', 1) // FLAG_GRANT_READ_URI_PERMISSION
      }
      resultHandler = (code: number, result: number, data: NativeObject) => {
        if (code !== requestCode) {
          if (typeof previousResult === 'function') previousResult.call(activity, code, result, data)
          return
        }
        // Detach before copying, but hold the busy lock until copying finishes.
        if (activity.onActivityResult === resultHandler) activity.onActivityResult = previousResult
        if (result !== -1 || !data) {
          cleanup()
          reject(new Error('cancel'))
          return
        }
        try {
          const uri = android.invoke(data, 'getData')
          if (!uri) throw new Error('No media was selected.')
          resolve(copySelectedMedia(uri, kind))
        } catch (error) {
          reject(error)
        } finally {
          cleanup()
        }
      }
      activity.onActivityResult = resultHandler
      android.invoke(activity, 'startActivityForResult', intent, requestCode)
    } catch (error) {
      cleanup()
      reject(error)
    }
  })
}

function copySelectedMedia(uri: NativeObject, kind: 'image' | 'video') {
  const android = plus!.android
  const activity = android.runtimeMainActivity()
  const resolver = android.invoke(activity, 'getContentResolver')
  const mime = String(android.invoke(resolver, 'getType', uri) || '')
  if (mime && !mime.startsWith(`${kind}/`)) throw new Error(`Please select a ${kind} file.`)
  const MimeTypeMap = android.importClass('android.webkit.MimeTypeMap')
  const extension = String(android.invoke(MimeTypeMap.getSingleton(), 'getExtensionFromMimeType', mime) || (kind === 'image' ? 'jpg' : 'mp4'))
    .replace(/[^a-zA-Z0-9]/g, '')
  const directory = android.newObject('java.io.File', android.invoke(activity, 'getCacheDir'), 'selected-media')
  android.invoke(directory, 'mkdirs')
  const file = android.newObject('java.io.File', directory, `${Date.now()}-${nextRequestCode}.${extension}`)
  let input: NativeObject
  let output: NativeObject
  let source: NativeObject
  let complete = false
  try {
    input = android.invoke(resolver, 'openInputStream', uri)
    if (!input) throw new Error('The selected file could not be opened.')
    output = android.newObject('java.io.FileOutputStream', file)
    const Channels = android.importClass('java.nio.channels.Channels')
    source = Channels.newChannel(input)
    const target = android.invoke(output, 'getChannel')
    let position = 0
    let copied = 0
    do {
      copied = Number(android.invoke(target, 'transferFrom', source, position, 8 * 1024 * 1024))
      position += copied
    } while (copied > 0)
    if (!position) throw new Error('The selected file is empty.')
    complete = true
    return `file://${android.invoke(file, 'getAbsolutePath')}`
  } finally {
    for (const stream of [source, input, output]) {
      if (stream) {
        try { android.invoke(stream, 'close') } catch { /* Close remaining streams too. */ }
      }
    }
    if (!complete) android.invoke(file, 'delete')
  }
}

function chooseSource(sources: string[] | undefined, camera: () => void, album: () => void, cancel: () => void) {
  if (sources?.length === 1) {
    if (sources[0] === 'camera') camera()
    else album()
    return
  }
  uni.showActionSheet({
    itemList: ['Choose from photos', 'Use camera'],
    success: ({ tapIndex }) => tapIndex === 0 ? album() : camera(),
    fail: cancel
  })
}

export function chooseUploadImage(options: UniApp.ChooseImageOptions) {
  if (!isAndroidApp()) { uni.chooseImage(options); return }
  const fail = (error: unknown) => {
    const result = { errMsg: `chooseImage:fail ${errorMessage(error)}` }
    options.fail?.(result)
    options.complete?.(result)
  }
  const album = async () => {
    try {
      if ((options.count || 1) > 1) throw new Error('Select one image at a time.')
      const path = await pickAndroidMedia('image')
      const size = await new Promise<number>((resolve, reject) => uni.getFileInfo({ filePath: path, success: info => resolve(info.size), fail: reject }))
      const result = { errMsg: 'chooseImage:ok', tempFilePaths: [path], tempFiles: [{ path, size }] }
      options.success?.(result)
      options.complete?.(result)
    } catch (error) { fail(error) }
  }
  chooseSource(options.sourceType, () => uni.chooseImage({ ...options, sourceType: ['camera'] }), album, () => fail('cancel'))
}

export function chooseUploadVideo(options: UniApp.ChooseVideoOptions) {
  if (!isAndroidApp()) { uni.chooseVideo(options); return }
  const fail = (error: unknown) => {
    const result = { errMsg: `chooseVideo:fail ${errorMessage(error)}` }
    options.fail?.(result)
    options.complete?.(result)
  }
  const album = async () => {
    try {
      const path = await pickAndroidMedia('video')
      const info = await new Promise<UniApp.GetVideoInfoSuccessData>((resolve, reject) => uni.getVideoInfo({ src: path, success: resolve, fail: reject }))
      // tempFile/name in uni's shared type are browser-only fields.
      const result = { errMsg: 'chooseVideo:ok', tempFilePath: path, duration: info.duration, size: info.size, width: info.width, height: info.height } as UniApp.ChooseVideoSuccess
      options.success?.(result)
      options.complete?.(result)
    } catch (error) { fail(error) }
  }
  chooseSource(options.sourceType, () => uni.chooseVideo({ ...options, sourceType: ['camera'] }), album, () => fail('cancel'))
}
