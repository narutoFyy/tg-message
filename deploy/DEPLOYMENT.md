# Gift Chat Deployment Guide

## 1. Server prerequisites

- A Linux VPS with Docker Engine and Docker Compose plugin installed
- One public IP; a domain is recommended for HTTPS and app distribution
- Open ports `80` and `443`

## 2. Copy the project to the server

```bash
git clone <your-repo-url> gift-chat
cd gift-chat/deploy
cp .env.example .env
```

Edit `.env` with real MySQL passwords, JWT secret, and your public origin values.

## 3. Start the stack

```bash
docker compose --env-file .env up -d --build
```

Services:

- `frontend`: Nginx serving the H5 bundle and reverse-proxying `/api`, `/ws`, and `/uploads`
- `backend`: Spring Boot app running with `prod` profile
- `mysql`: MySQL 8 persistent database
- `redis`: WebSocket fanout bus for multi-instance chat/read/video/presence events

## 4. Verify the deployment

```bash
curl http://YOUR_SERVER_IP/
curl http://YOUR_SERVER_IP/api/rates
```

Expected checks:

- Home page loads from `/`
- `/api/rates` returns JSON
- Login works from the browser
- Uploading a chat image or voice message stores a file under the uploads volume
- WebSocket upgrade path `/ws/chat/...` succeeds from the app

## 5. HTTPS before public release

If you have a domain:

1. Point the domain to the server IP.
2. Put a TLS terminator in front of Nginx, or extend the Nginx container with Certbot / reverse proxy tooling.
3. Update:
   - `APP_CORS_ALLOWED_ORIGINS`
   - `APP_UPLOAD_PUBLIC_BASE_URL`
   - `APP_REALTIME_REDIS_ENABLED`
   - Tencent Chat values if hybrid mobile delivery/push is enabled: `APP_TENCENT_CHAT_ENABLED`, `APP_TENCENT_CHAT_SDK_APP_ID`, `APP_TENCENT_CHAT_SECRET_KEY` or `APP_TENCENT_CHAT_ADMIN_USER_SIG`

For a public Android download, HTTPS is strongly recommended even if the first deployment uses only the bare IP.

Video calls require HTTPS in browser deployments. Browser camera/microphone APIs are blocked on plain `http://`
except for `localhost`, and TRTC Web SDK will not be able to publish local audio/video without media device access.
After enabling HTTPS, verify:

- The address bar shows a secure HTTPS domain, not a bare IP or HTTP URL.
- Browser site permissions allow Camera and Microphone for the domain.
- The browser is a supported modern WebRTC browser such as Chrome, Edge, Safari, or Firefox.
- No other app or browser tab is occupying the camera or microphone.
- `/ws/` still upgrades correctly through the HTTPS reverse proxy.

## 6. Operational notes

- Uploaded chat images, voucher images, and voice messages are stored in the `upload-data` Docker volume.
- Database state is stored in the `mysql-data` Docker volume.
- Redis fanout state is transient, but append-only persistence is enabled in the `redis-data` Docker volume for safer restarts.
- Tencent Chat hybrid mode is disabled by default; when enabled, local messages remain authoritative and Tencent failures are recorded for retry.
- Flyway runs automatically on backend startup.
- Production disables demo-user fallback and requires JWT access tokens.
