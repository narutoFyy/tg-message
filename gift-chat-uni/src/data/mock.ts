import type { ChatMessage, FriendProfile, RateItem, TransactionItem } from '@/types'

export const mockRates: RateItem[] = [
  { id: 'rate-1', cardName: 'Apple(itunes)', region: 'NG', rate: '1$ ≈ ₦1051.75', status: 'active', updatedAt: '2026-05-21 11:15' },
  { id: 'rate-2', cardName: 'Steam', region: 'NG', rate: '1$ ≈ ₦886.88', status: 'active', updatedAt: '2026-05-21 11:15' },
  { id: 'rate-3', cardName: 'Razer Gold', region: 'NG', rate: '1$ ≈ ₦1076.56', status: 'active', updatedAt: '2026-05-21 11:15' },
  { id: 'rate-4', cardName: 'Zelle', region: 'NG', rate: '1$ ≈ ₦552.77', status: 'active', updatedAt: '2026-05-21 11:15' },
  { id: 'rate-5', cardName: 'Chime', region: 'NG', rate: '1$ ≈ ₦1203.35', status: 'active', updatedAt: '2026-05-21 11:15' },
  { id: 'rate-6', cardName: 'Xbox', region: 'NG', rate: '1$ ≈ ₦890.67', status: 'active', updatedAt: '2026-05-21 11:15' },
  { id: 'rate-7', cardName: 'eBay', region: 'NG', rate: '1$ ≈ ₦740.96', status: 'active', updatedAt: '2026-05-21 11:15' },
  { id: 'rate-8', cardName: 'Sephora', region: 'NG', rate: '1$ ≈ ₦1053.64', status: 'active', updatedAt: '2026-05-21 11:15' },
  { id: 'rate-9', cardName: 'Google', region: 'NG', rate: '1$ ≈ ₦397.96', status: 'active', updatedAt: '2026-05-21 11:15' },
  { id: 'rate-10', cardName: 'Vanilla', region: 'NG', rate: '1$ ≈ ₦568.51', status: 'active', updatedAt: '2026-05-21 11:15' },
  { id: 'rate-11', cardName: 'American Express', region: 'NG', rate: '1$ ≈ ₦473.76', status: 'active', updatedAt: '2026-05-21 11:15' }
]

export const mockSupportMessages: ChatMessage[] = []

export const mockTransactions: TransactionItem[] = [
  {
    id: 'trade-1',
    orderNo: 'CB240527-001',
    cardName: 'Apple(itunes)',
    faceValue: '$200',
    payoutAmount: 'NGN 198500',
    status: 'pending',
    counterpartyName: 'Gift Hunter',
    counterpartyUsername: 'gift_hunter',
    friendshipId: 'friendship-1',
    note: 'Waiting for card screenshots and receipt confirmation.',
    voucherImageUrl: '',
    createdAt: '2026-05-27 09:15',
    updatedAt: '2026-05-27 09:15'
  },
  {
    id: 'trade-2',
    orderNo: 'CB240527-002',
    cardName: 'Steam',
    faceValue: '$100',
    payoutAmount: 'NGN 88400',
    status: 'processing',
    counterpartyName: 'Trade With Mina',
    counterpartyUsername: 'tradewithmina',
    friendshipId: 'friendship-2',
    note: 'Counterparty is checking the code balance before payout.',
    voucherImageUrl: '',
    createdAt: '2026-05-27 10:02',
    updatedAt: '2026-05-27 10:24'
  },
  {
    id: 'trade-3',
    orderNo: 'CB240527-003',
    cardName: 'Razer Gold',
    faceValue: '$50',
    payoutAmount: 'NGN 53200',
    status: 'completed',
    counterpartyName: 'Gift Hunter',
    counterpartyUsername: 'gift_hunter',
    friendshipId: 'friendship-1',
    note: 'Settled successfully this morning.',
    voucherImageUrl: '',
    createdAt: '2026-05-27 07:48',
    updatedAt: '2026-05-27 08:10'
  }
]

export const mockFriends: FriendProfile[] = [
  {
    id: 'friendship-1',
    username: 'gift_hunter',
    displayName: 'Gift Hunter',
    phone: '+234 809 000 1234',
    status: 'online',
    tags: ['Verified trader', 'Image and voice ready'],
    unreadCount: 1,
    messages: [
      {
        id: 'dm-1',
        author: 'friend',
        content: 'I saw you are trading Apple cards too. Can we compare today\'s rate later?',
        type: 'text',
        createdAt: '05-21 11:15'
      },
      {
        id: 'dm-2',
        author: 'me',
        content: 'Sure. I will sort today\'s rate list first and send it over.',
        type: 'text',
        createdAt: '05-21 11:16'
      }
    ]
  },
  {
    id: 'friendship-2',
    username: 'tradewithmina',
    displayName: 'Trade With Mina',
    phone: '+44 7400 221199',
    status: 'online',
    tags: ['Trusted contact', 'Fast settlement'],
    unreadCount: 0,
    messages: [
      {
        id: 'dm-3',
        author: 'friend',
        content: '[image placeholder]',
        type: 'image',
        createdAt: '05-21 11:15'
      }
    ]
  }
]

export const mockBlacklist: FriendProfile[] = [
  {
    id: 'black-1',
    username: 'spam_rate_88',
    displayName: 'spam_rate_88',
    phone: '+86 135 0000 2222',
    status: 'blocked',
    tags: ['Blocked from chat'],
    unreadCount: 0,
    messages: []
  }
]
