# CardBrother Uni App

`gift-chat-uni` is the active frontend for the CardBrother production MVP.

It targets:

- H5 deployment
- Android/iOS app packaging through uni-app
- A shared UI layer for rates, support chat, private chat, transactions, blacklist, profile, and admin rate management

## Current feature set

- Password login and registration
- JWT-backed API access
- Home rate board
- Customer service chat
- Friend search, friend requests, and private chat
- Transaction list, creation, and status flow
- Image upload for chat and trade vouchers
- Blacklist management
- Me / Wallet / Performance / Settings pages
- Admin rate creation and status toggle
- Admin console for users, agents, and support assignments

## Main pages

- `pages/login/index`
- `pages/register/index`
- `pages/home/index`
- `pages/support/index`
- `pages/friends/index`
- `pages/transactions/index`
- `pages/me/index`
- `pages/wallet/index`
- `pages/performance/index`
- `pages/settings/index`
- `pages/blacklist/index`
- `pages/admin-rates/index`
- `pages/admin-console/index`

## Environment configuration

The frontend uses environment-driven backend endpoints.

- `.env.development`
  - `VITE_API_BASE_URL=http://127.0.0.1:8081/api`
  - `VITE_WS_BASE_URL=ws://127.0.0.1:8081/ws`
- `.env.production`
  - `VITE_API_BASE_URL=/api`
  - `VITE_WS_BASE_URL=/ws`
  - `VITE_ALLOW_DEMO_FALLBACK=false`

`VITE_ALLOW_DEMO_FALLBACK=true` can be used for an internal demo build. Production builds should leave it disabled so Guest entry and mock fallback are not exposed.

## Local development

```bash
npm install
npm run dev:h5 -- --host 127.0.0.1 --port 5174
```

Useful commands:

- `npm run type-check`
- `npm run build:h5`
- `npm run build:app`

## Local URLs

- H5: [http://127.0.0.1:5174](http://127.0.0.1:5174)
- Backend API: [http://127.0.0.1:8081](http://127.0.0.1:8081)

## Demo accounts

- User
  - username: `cardnova_user`
  - email: `demo@cardnova.app`
  - phone: `13800138000`
  - password: `demo12345`
- Agent
  - username: `support_luna`
  - email: `luna@cardnova.app`
  - password: `demo12345`

## Packaging

See [APP_PACKAGING.md](/D:/cyproject/tg-message/gift-chat-uni/APP_PACKAGING.md) for Android and iOS packaging notes.
