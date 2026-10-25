import { readFileSync, mkdirSync, writeFileSync } from 'node:fs'
const root = new URL('../', import.meta.url)
const legal = JSON.parse(readFileSync(new URL('src/data/legal.json', root), 'utf8'))
const out = new URL('src/static/legal/', root)
mkdirSync(out, { recursive: true })
const escape = value => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;')
for (const kind of ['privacy', 'terms']) {
  const doc = legal[kind]
  writeFileSync(new URL(`${kind}.html`, out), `<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Xcard ${escape(doc.title)}</title><style>body{max-width:720px;margin:0 auto;padding:24px;color:#17212b;background:white;font:16px/1.75 system-ui,sans-serif}h1{font-size:28px}h2{font-size:20px;margin-top:28px}p{white-space:pre-line;overflow-wrap:anywhere}</style><h1>${escape(doc.title)}</h1><p>Updated ${escape(legal.updated)}</p>${doc.sections.map(s => `<h2>${escape(s.title)}</h2><p>${escape(s.body)}</p>`).join('')}</html>`)
}
const privacy = {
  version: legal.version, prompt: 'template', title: 'Your privacy at Xcard',
  message: 'Please read our <a href="static/legal/terms.html">Terms of Use</a> and <a href="static/legal/privacy.html">Privacy Policy</a>. We process account details, messages and transaction information to provide the service. Optional camera and microphone access is requested when you use those features. Android photos and videos are selected through the system picker. Tap Agree and continue to use Xcard.',
  buttonAccept: 'Agree and continue', buttonRefuse: 'Disagree', hrefLoader: 'default', backToExit: 'true',
  second: { title: 'Privacy agreement required', message: 'Please review our <a href="static/legal/privacy.html">Privacy Policy</a> before continuing. If you disagree, you can exit Xcard.', buttonAccept: 'Agree and continue', buttonRefuse: 'Exit' },
  disagreeMode: { support: false, loadNativePlugins: false, visitorEntry: false, showAlways: false },
  styles: { backgroundColor: '#ffffff', borderRadius: '20px', title: { color: '#17212b' }, buttonAccept: { color: '#2368c4' }, buttonRefuse: { color: '#667483' } }
}
writeFileSync(new URL('src/androidPrivacy.json', root), JSON.stringify(privacy, null, 2) + '\n')
