export type GiftCardLogoKey =
  | 'apple'
  | 'steam'
  | 'razer'
  | 'xbox'
  | 'ebay'
  | 'sephora'
  | 'google'
  | 'vanilla'
  | 'amex'
  | 'zelle'
  | 'chime'

export interface GiftCardDefinition {
  code: string
  name: string
  aliases: readonly string[]
  logoKey: GiftCardLogoKey
}

export const giftCardCatalog: readonly GiftCardDefinition[] = [
  card('APPLE_ITUNES', 'Apple / iTunes', 'apple', ['Apple', 'iTunes', 'Apple(itunes)', 'Apple iTunes']),
  card('STEAM', 'Steam', 'steam', ['Steam Card', 'Steam Gift Card']),
  card('RAZER_GOLD', 'Razer Gold', 'razer', ['Razer', 'RazerGold']),
  card('XBOX', 'Xbox', 'xbox', ['Xbox Card', 'Xbox Gift Card']),
  card('EBAY', 'eBay', 'ebay', ['Ebay', 'eBay Card']),
  card('SEPHORA', 'Sephora', 'sephora', ['Sephora Card']),
  card('GOOGLE_PLAY', 'Google Play', 'google', ['Google', 'Google Card', 'Google Play Card']),
  card('VANILLA', 'Vanilla', 'vanilla', ['Vanilla Card', 'Vanilla Gift Card']),
  card('AMERICAN_EXPRESS', 'American Express', 'amex', ['Amex', 'American Express Card']),
  card('ZELLE', 'Zelle', 'zelle', ['Zelle Card']),
  card('CHIME', 'Chime', 'chime', ['Chime Card'])
]

const cardsByCode = new Map(giftCardCatalog.map((item) => [item.code, item]))
const cardsByAlias = new Map<string, GiftCardDefinition>()

for (const item of giftCardCatalog) {
  for (const alias of [item.code, item.name, ...item.aliases]) {
    cardsByAlias.set(normalizeGiftCardText(alias), item)
  }
}

export function findGiftCardByCode(code?: string | null) {
  return cardsByCode.get((code || '').trim().toUpperCase())
}

export function findGiftCardByName(name?: string | null) {
  return cardsByAlias.get(normalizeGiftCardText(name || ''))
}

export function resolveGiftCard(cardCode?: string | null, cardName?: string | null) {
  return findGiftCardByCode(cardCode) || findGiftCardByName(cardName)
}

export function matchesGiftCardSearch(card: GiftCardDefinition, query: string) {
  const normalizedQuery = query.trim().toLowerCase()
  if (!normalizedQuery) return true
  return [card.name, card.code, ...card.aliases].some((value) => value.toLowerCase().includes(normalizedQuery))
}

function card(code: string, name: string, logoKey: GiftCardLogoKey, aliases: readonly string[]): GiftCardDefinition {
  return { code, name, aliases, logoKey }
}

function normalizeGiftCardText(value: string) {
  return value.trim().toUpperCase().replace(/[^A-Z0-9]/g, '')
}
