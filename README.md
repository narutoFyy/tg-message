# CardBrother Gift Chat

CardBrother Gift Chat is a gift-card rate, order, wallet, and one-to-one customer-service chat system. The project contains a Spring Boot backend and a uni-app/Vue frontend, with support for realtime IM, media messages, sell-card flows, withdrawal/loan requests, customer-service workbench operations, and TRTC video calls.

## Modules

- `gift-chat-server`: Spring Boot backend, Java 21, REST APIs, WebSocket chat, JPA/Flyway, H2 dev profile, MySQL runtime support.
- `gift-chat-uni`: uni-app/Vue frontend for H5 and app builds.
- `DEPLOY_CHECKLIST.md`: deployment and smoke-test checklist.
- Product and delivery notes are kept in the project root as markdown documents.

## Main Features

- User login/register routing by role.
- Gift-card rate list, sell-card order draft, voucher image attachment, and order messaging to support.
- Wallet, withdrawal request, loan request, transaction and profile pages.
- Customer-service one-to-one IM with text, image, GIF, voice, read status, unread handling, reconnect heartbeat, and media fallback UI.
- Support workbench with customer list, active conversation, customer profile, orders, loans, withdrawals, notes, and language toggle.
- Support-sent Chinese text can be translated to English before sending to customers.
- TRTC video call entry and call-status UI.
- Product UI pass with desktop/mobile responsive layout, themed login/home/chat backgrounds, and media bubble sizing by image aspect ratio.

## Local Development

Backend:

```powershell
cd W:\cyproject\10-tg-message\tg-message\gift-chat-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected backend port:

```text
8081
```

Frontend:

```powershell
cd W:\cyproject\10-tg-message\tg-message\gift-chat-uni
npm install
npm run dev:h5
```

Expected frontend port:

```text
5174
```

Open:

```text
http://127.0.0.1:5174/#/
```

## Verification

Frontend type check:

```powershell
cd W:\cyproject\10-tg-message\tg-message\gift-chat-uni
npm run type-check
```

Frontend H5 build:

```powershell
cd W:\cyproject\10-tg-message\tg-message\gift-chat-uni
npm run build:h5
```

Backend tests:

```powershell
cd W:\cyproject\10-tg-message\tg-message\gift-chat-server
mvn test
```

## Demo Accounts

Common dev accounts:

| Role | Username | Password |
| --- | --- | --- |
| User | `cardnova_user` | `demo12345` |
| Support | `support_luna` | `demo12345` |
| Support | `support_angela` | `demo12345` |
| Admin | `admin_mia` | `demo12345` |

Seed data may vary by environment. If login fails, check backend seed SQL or the active database.

## Production Deployment

The current server plan uses:

- Frontend: `8086`
- Backend: `8087`

Recommended public access:

```text
http://<server-ip>:8086
```

If frontend and backend are served by the same domain or gateway, proxy these paths to the backend:

- `/api/**`
- `/ws/**`
- `/uploads/images/**`
- `/uploads/voices/**`

If frontend calls backend directly on `8087`, confirm the server firewall allows public access to `8087`.

See `DEPLOY_CHECKLIST.md` for full Nginx/gateway examples and smoke-test commands.

## Runtime Configuration

Important backend environment variables:

- `APP_UPLOAD_IMAGE_DIR`: local image upload directory.
- `APP_UPLOAD_VOICE_DIR`: local voice upload directory.
- `APP_UPLOAD_PUBLIC_BASE_URL`: empty for same-domain `/uploads/...` URLs, or the public backend origin for split-port deployment.
- `APP_VIDEO_VENDOR=trtc`
- `APP_VIDEO_TRTC_SDK_APP_ID`
- `APP_VIDEO_TRTC_SECRET_KEY`
- `APP_VIDEO_TRTC_USER_SIG_EXPIRE_SECONDS`

`APP_VIDEO_TRTC_SECRET_KEY` must stay backend-only. Do not put real TRTC secrets in frontend code or committed files.

## Delivery Smoke Checklist

Before customer delivery, confirm:

- User and support can stay logged in at the same time in different browsers.
- User messages do not route to the support page unless the logged-in account role is support/admin.
- Text, image, GIF, voice, sell-card order, withdrawal request, and loan request appear in the correct one-to-one conversation.
- Support-sent Chinese text reaches the customer as English when translation is enabled.
- Sent messages show one check; read messages show two checks on the same horizontal line with the timestamp.
- Image/GIF previews keep the original aspect ratio and do not crop the right edge.
- Server can serve `/uploads/images/**` and `/uploads/voices/**` with HTTP 200.
- Browser console has no WebSocket, upload, or mixed-content errors.

## Notes

- Chat media rendering is centralized in `gift-chat-uni/src/components/chat/MediaMessage.vue`.
- Backend chat and support data should remain server-backed, not page-local only.
- Keep deployment smoke tests in `DEPLOY_CHECKLIST.md` updated when ports, domains, or gateway rules change.
