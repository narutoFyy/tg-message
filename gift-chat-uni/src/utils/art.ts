const compactSvg = (svg: string) => svg.replace(/\s{2,}/g, ' ').replace(/>\s+</g, '><').trim()

const svgToDataUri = (svg: string) => `data:image/svg+xml;utf8,${encodeURIComponent(compactSvg(svg))}`

export const uiIcons = {
  back: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#111111" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
      <path d="M15 4 7 12l8 8"/>
    </svg>
  `),
  close: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#111111" stroke-width="2.4" stroke-linecap="round">
      <path d="M5 5l14 14M19 5 5 19"/>
    </svg>
  `),
  share: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#111111" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="18" cy="5" r="2.8"/>
      <circle cx="6" cy="12" r="2.8"/>
      <circle cx="18" cy="19" r="2.8"/>
      <path d="M8.7 10.8 15.2 6.6M8.7 13.2l6.5 4.2"/>
    </svg>
  `),
  settings: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#111111" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">
      <path d="M12 3.7 19 7.6v8.8l-7 3.9-7-3.9V7.6l7-3.9Z"/>
      <circle cx="12" cy="12" r="3.3"/>
    </svg>
  `),
  copy: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#111111" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">
      <rect x="8" y="7" width="10" height="12" rx="2"/>
      <path d="M6 15H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v1"/>
    </svg>
  `),
  chevronDown: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#1c1c1c" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
      <path d="m6 9 6 6 6-6"/>
    </svg>
  `),
  scammer: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" fill="none" stroke="#111111" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="25" cy="18" r="8"/>
      <path d="M12 44c2.5-8 8-12 13-12s10.5 4 13 12"/>
      <circle cx="45" cy="39" r="10" fill="#15d66f" stroke="none"/>
      <path d="m39 45 12-12" stroke="#ffffff" stroke-width="4"/>
    </svg>
  `),
  staff: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" fill="none" stroke-linecap="round" stroke-linejoin="round">
      <path d="M32 8 50 14v12c0 13-8.5 23-18 30-9.5-7-18-17-18-30V14l18-6Z" stroke="#111111" stroke-width="3"/>
      <path d="m24 32 6 6 11-13" stroke="#14d86f" stroke-width="4"/>
    </svg>
  `),
  wallet: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" fill="none" stroke-linecap="round" stroke-linejoin="round">
      <rect x="10" y="18" width="44" height="32" rx="9" fill="#15d66f" stroke="#111111" stroke-width="3"/>
      <path d="M16 18c4-7 11-10 20-9" stroke="#111111" stroke-width="3"/>
      <path d="M42 31h12v10H42a5 5 0 0 1 0-10Z" fill="#ffffff" stroke="#111111" stroke-width="3"/>
      <circle cx="43" cy="36" r="2" fill="#111111"/>
    </svg>
  `),
  user: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">
      <circle cx="24" cy="24" r="24" fill="#f0f1f2"/>
      <circle cx="24" cy="17" r="7" fill="#d7dade"/>
      <path d="M12 37c2.6-6.4 7-9.5 12-9.5S33.4 30.6 36 37" fill="#d7dade"/>
    </svg>
  `)
}

export const navIcons = {
  home: {
    active: svgToDataUri(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">
        <path d="M24 6 38 16v19a4 4 0 0 1-4 4H14a4 4 0 0 1-4-4V16L24 6Z" fill="#15d66f"/>
        <rect x="21" y="24" width="6" height="12" rx="2" fill="#ffffff" opacity="0.9"/>
      </svg>
    `),
    inactive: svgToDataUri(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" fill="none" stroke="#c8d0d8" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
        <path d="M7 21 24 8l17 13"/>
        <path d="M12 19v18a3 3 0 0 0 3 3h18a3 3 0 0 0 3-3V19"/>
      </svg>
    `)
  },
  chat: {
    active: svgToDataUri(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">
        <path d="M24 10c8.8 0 16 6.4 16 14.4 0 7.8-6.8 14.1-15.2 14.4L16 43l2.5-5.5C13 35.7 8 30.6 8 24.4 8 16.4 15.2 10 24 10Z" fill="#15d66f"/>
        <circle cx="18" cy="25" r="2.3" fill="#ffffff"/>
        <circle cx="24" cy="25" r="2.3" fill="#ffffff"/>
        <circle cx="30" cy="25" r="2.3" fill="#ffffff"/>
      </svg>
    `),
    inactive: svgToDataUri(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" fill="none" stroke="#c8d0d8" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
        <path d="M24 10c8.8 0 16 6.4 16 14.4 0 7.8-6.8 14.1-15.2 14.4L16 43l2.5-5.5C13 35.7 8 30.6 8 24.4 8 16.4 15.2 10 24 10Z"/>
      </svg>
    `)
  },
  swap: {
    active: svgToDataUri(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">
        <path d="M11 17h20l-5-5" fill="none" stroke="#15d66f" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M37 31H17l5 5" fill="none" stroke="#15d66f" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    `),
    inactive: svgToDataUri(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">
        <path d="M11 17h20l-5-5" fill="none" stroke="#c8d0d8" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M37 31H17l5 5" fill="none" stroke="#c8d0d8" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    `)
  },
  me: {
    active: svgToDataUri(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">
        <circle cx="24" cy="15" r="8" fill="#15d66f"/>
        <path d="M9 39c3-8.5 8.8-13 15-13s12 4.5 15 13" fill="#15d66f"/>
      </svg>
    `),
    inactive: svgToDataUri(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" fill="none" stroke="#c8d0d8" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="24" cy="15" r="7"/>
        <path d="M11 38c2.7-7.5 7.7-11.5 13-11.5S34.3 30.5 37 38"/>
      </svg>
    `)
  }
}

export const pageArt = {
  homeBanner: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 160">
      <defs>
        <linearGradient id="coin" x1="0" x2="1">
          <stop offset="0" stop-color="#fff1a9"/>
          <stop offset="1" stop-color="#ffb84a"/>
        </linearGradient>
        <linearGradient id="shine" x1="0" x2="1">
          <stop offset="0" stop-color="#ffffff" stop-opacity="0.75"/>
          <stop offset="1" stop-color="#ffffff" stop-opacity="0"/>
        </linearGradient>
      </defs>
      <ellipse cx="86" cy="28" rx="46" ry="18" fill="#ffffff" opacity="0.12"/>
      <ellipse cx="290" cy="26" rx="68" ry="18" fill="#ffffff" opacity="0.15"/>
      <circle cx="328" cy="118" r="34" fill="url(#coin)" opacity="0.92"/>
      <circle cx="268" cy="98" r="28" fill="url(#coin)" opacity="0.78"/>
      <circle cx="304" cy="54" r="10" fill="#fff9d8" opacity="0.7"/>
      <path d="M222 122c7-22 19-35 36-38 16 3 27 16 33 38" fill="#422765" opacity="0.95"/>
      <path d="M246 84c-8 0-15 6-17 18l-3 20h22l-1-22c-1-10-5-16-11-16Z" fill="#80f0a4"/>
      <circle cx="240" cy="73" r="10" fill="#522e74"/>
      <path d="M269 78c-8 0-15 7-16 20l1 24h26l-2-25c-2-12-5-19-9-19Z" fill="#2c7af3"/>
      <circle cx="266" cy="67" r="11" fill="#5c367f"/>
      <path d="M299 85c8 0 14 6 17 18l3 19h-23l1-21c1-10 5-16 11-16Z" fill="#67d68d"/>
      <circle cx="302" cy="74" r="10" fill="#5a356f"/>
      <path d="M210 80c9 3 16 9 22 18" stroke="#5b2f73" stroke-width="4" stroke-linecap="round"/>
      <path d="M319 82c-7 1-14 7-19 16" stroke="#5b2f73" stroke-width="4" stroke-linecap="round"/>
      <path d="M0 112c24 10 43 8 58-4" fill="none" stroke="url(#shine)" stroke-width="12" opacity="0.7"/>
      <path d="M142 150c46-4 105-2 160-16" fill="none" stroke="#ffffff" stroke-opacity="0.18" stroke-width="8"/>
    </svg>
  `),
  profileAvatar: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 180 180">
      <defs>
        <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#dff87c"/>
          <stop offset="1" stop-color="#bdf0b0"/>
        </linearGradient>
      </defs>
      <circle cx="90" cy="90" r="84" fill="#ffffff"/>
      <circle cx="90" cy="90" r="77" fill="url(#bg)"/>
      <rect x="53" y="110" width="74" height="42" rx="18" fill="#bfe0ff"/>
      <rect x="75" y="92" width="30" height="25" rx="9" fill="#ffd5b3"/>
      <ellipse cx="90" cy="73" rx="31" ry="35" fill="#ffd9b8"/>
      <path d="M58 71c0-23 13-37 34-40 15-2 30 4 40 16l-2 13-8 5-7-10-5 9-11-8-13 7-4-10-7 18-17 7Z" fill="#4b58d0"/>
      <rect x="64" y="72" width="18" height="18" rx="6" fill="none" stroke="#ff7e61" stroke-width="4"/>
      <rect x="98" y="72" width="18" height="18" rx="6" fill="none" stroke="#ff7e61" stroke-width="4"/>
      <path d="M82 81h16" stroke="#ff7e61" stroke-width="4" stroke-linecap="round"/>
      <circle cx="80" cy="81" r="3" fill="#2f3555"/>
      <circle cx="103" cy="81" r="3" fill="#2f3555"/>
      <path d="M85 98c4 4 9 4 14 0" stroke="#d18772" stroke-width="3" stroke-linecap="round"/>
    </svg>
  `),
  vipBadge: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 84 64">
      <defs>
        <linearGradient id="core" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#fff5da"/>
          <stop offset="1" stop-color="#ffc98a"/>
        </linearGradient>
      </defs>
      <path d="M10 31 2 22l3-8 12 4 7 13H10Z" fill="#fff1dc"/>
      <path d="M74 31 82 22l-3-8-12 4-7 13h14Z" fill="#fff1dc"/>
      <path d="M42 8 61 18v19L42 50 23 37V18L42 8Z" fill="url(#core)" stroke="#f2b36d" stroke-width="3"/>
      <path d="m42 20 4.7 8 8.8 1.3-6.4 6.1 1.5 8.7L42 40l-8.6 4.1 1.5-8.7-6.4-6.1 8.8-1.3L42 20Z" fill="#ffcf75"/>
    </svg>
  `),
  wallet: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 130 130">
      <defs>
        <linearGradient id="w1" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#ffe8bb"/>
          <stop offset="1" stop-color="#ff9f43"/>
        </linearGradient>
      </defs>
      <rect x="28" y="32" width="72" height="58" rx="14" fill="url(#w1)" transform="rotate(18 64 61)"/>
      <rect x="38" y="42" width="52" height="38" rx="10" fill="#ffdcb0" opacity="0.92" transform="rotate(18 64 61)"/>
      <circle cx="73" cy="61" r="5" fill="#fff4df"/>
      <path d="M34 30c9-8 18-11 29-10" stroke="#fff8ea" stroke-width="6" stroke-linecap="round"/>
    </svg>
  `),
  friend: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 130 130">
      <defs>
        <radialGradient id="g" cx="50%" cy="40%" r="60%">
          <stop offset="0" stop-color="#e9fff4"/>
          <stop offset="1" stop-color="#8df3bf"/>
        </radialGradient>
      </defs>
      <circle cx="90" cy="34" r="18" fill="#d6fff0" opacity="0.8"/>
      <circle cx="72" cy="54" r="18" fill="url(#g)"/>
      <path d="M40 103c4-19 16-29 32-29s28 10 32 29" fill="#44d983" opacity="0.92"/>
      <circle cx="72" cy="58" r="21" fill="#4fd68a"/>
      <path d="M56 59c4-6 10-9 16-9s12 3 16 9" fill="none" stroke="#7ff0b0" stroke-width="4" stroke-linecap="round"/>
    </svg>
  `),
  trophy: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 220 150">
      <defs>
        <linearGradient id="cup" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#ff8f74"/>
          <stop offset="1" stop-color="#ffb449"/>
        </linearGradient>
        <linearGradient id="wing" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#fff7de"/>
          <stop offset="1" stop-color="#ffe2ab"/>
        </linearGradient>
      </defs>
      <path d="M56 44c-16 2-28 12-28 27 0 17 15 27 37 26" fill="none" stroke="url(#wing)" stroke-width="10" stroke-linecap="round"/>
      <path d="M164 44c16 2 28 12 28 27 0 17-15 27-37 26" fill="none" stroke="url(#wing)" stroke-width="10" stroke-linecap="round"/>
      <path d="M78 30h64c0 32-11 55-32 67-21-12-32-35-32-67Z" fill="url(#cup)"/>
      <rect x="95" y="96" width="30" height="17" rx="6" fill="#ff9b54"/>
      <rect x="80" y="111" width="60" height="11" rx="5.5" fill="#ffc978"/>
      <path d="m110 46 7 13 14 2-10 10 3 14-14-7-14 7 3-14-10-10 14-2 7-13Z" fill="#ffe79f"/>
      <circle cx="54" cy="28" r="4" fill="#ff8b62"/>
      <circle cx="171" cy="26" r="4" fill="#ff8b62"/>
      <circle cx="184" cy="62" r="5" fill="#ffd27a"/>
      <circle cx="35" cy="58" r="5" fill="#ffd27a"/>
    </svg>
  `)
}

const cardLogoMap = {
  apple: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <circle cx="36" cy="36" r="35" fill="#ffffff"/>
      <path d="M23 14h26v9H23z" fill="#6fc84a"/>
      <path d="M23 23h26v9H23z" fill="#f4d84f"/>
      <path d="M23 32h26v9H23z" fill="#ff922e"/>
      <path d="M23 41h26v9H23z" fill="#ff4d69"/>
      <path d="M23 50h26v9H23z" fill="#5e7bff"/>
      <path d="M38 18c2-4 5-6 9-6-1 4-3 7-6 8" fill="#6fc84a"/>
      <path d="M35 24c7 0 11 5 11 12 0 9-5 18-12 18s-11-9-11-17c0-7 5-13 12-13Z" fill="#ffffff" opacity="0.92"/>
      <path d="M39 22c-2-2-4-4-4-7 4 0 6 2 7 4" fill="#ffffff"/>
    </svg>
  `),
  steam: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <defs>
        <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#1e3257"/>
          <stop offset="1" stop-color="#0d6ab2"/>
        </linearGradient>
      </defs>
      <circle cx="36" cy="36" r="34" fill="url(#g)"/>
      <circle cx="47" cy="24" r="10" fill="#ffffff"/>
      <circle cx="47" cy="24" r="5" fill="#1e3257"/>
      <path d="M18 41 29 47c5 3 11 1 13-4l5-11" fill="none" stroke="#ffffff" stroke-width="5" stroke-linecap="round"/>
      <circle cx="29" cy="47" r="6" fill="#ffffff"/>
    </svg>
  `),
  razer: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <defs>
        <radialGradient id="g" cx="50%" cy="40%" r="60%">
          <stop offset="0" stop-color="#ffe694"/>
          <stop offset="1" stop-color="#d89b20"/>
        </radialGradient>
      </defs>
      <circle cx="36" cy="36" r="33" fill="url(#g)" stroke="#b97a11" stroke-width="2"/>
      <circle cx="36" cy="36" r="22" fill="#f0c15b" opacity="0.45"/>
      <text x="36" y="42" text-anchor="middle" font-size="25" font-weight="800" fill="#7f5413">R</text>
    </svg>
  `),
  xbox: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <circle cx="36" cy="36" r="33" fill="#f3f6f1" stroke="#dfe4dc" stroke-width="2"/>
      <path d="M20 20c7 1 12 5 16 11 4-6 9-10 16-11" fill="none" stroke="#7ccb47" stroke-width="6" stroke-linecap="round"/>
      <path d="M21 53c5-7 10-12 15-17 5 5 10 10 15 17" fill="none" stroke="#7ccb47" stroke-width="6" stroke-linecap="round"/>
    </svg>
  `),
  ebay: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <circle cx="36" cy="36" r="33" fill="#ffffff" stroke="#eef1f3" stroke-width="2"/>
      <text x="22" y="42" font-size="18" font-weight="800" fill="#ea4335">e</text>
      <text x="30" y="42" font-size="18" font-weight="800" fill="#4285f4">b</text>
      <text x="39" y="42" font-size="18" font-weight="800" fill="#fbbc05">a</text>
      <text x="49" y="42" font-size="18" font-weight="800" fill="#34a853">y</text>
    </svg>
  `),
  sephora: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <circle cx="36" cy="36" r="33" fill="#060606"/>
      <path d="M37 15c4 11 5 23 4 41" stroke="#ffffff" stroke-width="4" stroke-linecap="round"/>
      <text x="36" y="58" text-anchor="middle" font-size="7" font-weight="700" fill="#ffffff" letter-spacing="1">SEPHORA</text>
    </svg>
  `),
  google: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <path d="M17 16 55 36 17 56V16Z" fill="#34a853"/>
      <path d="M17 16 37 35 17 56 8 48 22 36 8 24Z" fill="#fbbc04"/>
      <path d="M37 35 17 16l22 11" fill="#4285f4"/>
      <path d="M37 35 17 56l22-11" fill="#ea4335"/>
    </svg>
  `),
  vanilla: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <circle cx="36" cy="36" r="33" fill="#f62534"/>
      <text x="36" y="32" text-anchor="middle" font-size="11" font-weight="700" fill="#ffffff">Vanilla</text>
      <text x="36" y="44" text-anchor="middle" font-size="8" fill="#ffffff">Gift Card</text>
    </svg>
  `),
  amex: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <circle cx="36" cy="36" r="33" fill="#2286e6"/>
      <text x="36" y="31" text-anchor="middle" font-size="9" font-weight="800" fill="#ffffff">AMERICAN</text>
      <text x="36" y="43" text-anchor="middle" font-size="12" font-weight="800" fill="#ffffff">EXPRESS</text>
    </svg>
  `),
  zelle: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <circle cx="36" cy="36" r="33" fill="#ffffff"/>
      <text x="36" y="43" text-anchor="middle" font-size="22" font-weight="800" fill="#5e33ad">zelle</text>
    </svg>
  `),
  chime: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <circle cx="36" cy="36" r="33" fill="#16c66c"/>
      <text x="36" y="43" text-anchor="middle" font-size="34" font-weight="800" fill="#eaf8ef">C</text>
    </svg>
  `),
  fallback: svgToDataUri(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
      <circle cx="36" cy="36" r="33" fill="#eef3f6"/>
      <text x="36" y="43" text-anchor="middle" font-size="22" font-weight="800" fill="#667085">Card</text>
    </svg>
  `)
}

export function cardLogoFor(name: string) {
  const normalized = name.toLowerCase()

  if (normalized.includes('apple')) return cardLogoMap.apple
  if (normalized.includes('steam')) return cardLogoMap.steam
  if (normalized.includes('razer')) return cardLogoMap.razer
  if (normalized.includes('xbox')) return cardLogoMap.xbox
  if (normalized.includes('ebay')) return cardLogoMap.ebay
  if (normalized.includes('sephora')) return cardLogoMap.sephora
  if (normalized.includes('google')) return cardLogoMap.google
  if (normalized.includes('vanilla')) return cardLogoMap.vanilla
  if (normalized.includes('american')) return cardLogoMap.amex
  if (normalized.includes('zelle')) return cardLogoMap.zelle
  if (normalized.includes('chime')) return cardLogoMap.chime

  return cardLogoMap.fallback
}
