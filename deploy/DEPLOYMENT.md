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

For a public Android download, HTTPS is strongly recommended even if the first deployment uses only the bare IP.

## 6. Operational notes

- Uploaded chat images, voucher images, and voice messages are stored in the `upload-data` Docker volume.
- Database state is stored in the `mysql-data` Docker volume.
- Flyway runs automatically on backend startup.
- Production disables demo-user fallback and requires JWT access tokens.
