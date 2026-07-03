# Deployment Checklist

Use this before handing the chat build to a customer or after each server redeploy.

## Ports

- Frontend: `8086`
- Backend: `8087`
- The browser should open the frontend URL only, for example `http://<server-ip>:8086` or `https://<your-domain>`.
- Frontend build should point API and WebSocket traffic to the public backend route:
  - Same-domain gateway: `VITE_API_BASE_URL=/api`, `VITE_WS_BASE_URL=/ws`
  - Split ports: `VITE_API_BASE_URL=http://<server-ip>:8087/api`, `VITE_WS_BASE_URL=ws://<server-ip>:8087/ws`

## Nginx Or Gateway

If frontend and backend are served through the same domain, proxy these paths to the backend:

```nginx
location /api/ {
  proxy_pass http://127.0.0.1:8087/api/;
}

location /ws/ {
  proxy_pass http://127.0.0.1:8087/ws/;
  proxy_http_version 1.1;
  proxy_set_header Upgrade $http_upgrade;
  proxy_set_header Connection "upgrade";
  proxy_set_header Host $host;
}

location /uploads/images/ {
  proxy_pass http://127.0.0.1:8087/uploads/images/;
}

location /uploads/voices/ {
  proxy_pass http://127.0.0.1:8087/uploads/voices/;
}
```

If frontend uses backend port `8087` directly, confirm the server firewall allows external access to `8087`.

## Backend Upload Settings

- `APP_UPLOAD_IMAGE_DIR` points to the image storage directory.
- `APP_UPLOAD_VOICE_DIR` points to the voice storage directory.
- `APP_UPLOAD_PUBLIC_BASE_URL` is empty for same-domain `/uploads/...` links, or set to the public backend origin if using split ports.
- The backend process user can read and write both upload directories.

## Referral Rewards

- Back up the production MySQL database before the Flyway migration runs.
- Confirm every existing `app_user` row has a non-empty `invite_code` after migration.
- In the admin console, verify the registration cashback and trade rebate percentage before opening registration to users.
- Run one live smoke path with a test inviter and a test new user: register with invite code, complete one trade, then confirm exactly one registration reward and one trade rebate reward appear in admin records.
- Keep the previous backend image/tag and database backup available before enabling paid referrals for real traffic.

## Smoke Tests

Run these from your local machine after deployment:

```powershell
Invoke-WebRequest http://<server-ip>:8086/ -UseBasicParsing
Invoke-WebRequest http://<server-ip>:8087/api/health -UseBasicParsing
```

If the backend exits during startup, first check that `APP_AUTH_JWT_SECRET` is a long random value and `APP_CORS_ALLOWED_ORIGINS` contains only the real production domain/IP.

After sending one image in chat, copy the image URL from browser DevTools and test it directly:

```powershell
Invoke-WebRequest http://<server-ip>:8087/uploads/images/<file-name>.png -UseBasicParsing
```

Expected result:

- Status is `200`.
- Content-Type starts with `image/` for images or GIFs.
- Browser console has no `ERR_CONNECTION_REFUSED`, `404`, or mixed-content error for `/uploads/images/**`.
- Browser console has no WebSocket failure for `/ws/chat/**`.
- The chat page shows a visible media failure card instead of a blank bubble if a resource is unavailable.

## Customer Acceptance Pass

- User and support can stay logged in at the same time in different browsers.
- User text, support text, image, GIF, voice, sell order, withdrawal request, and loan request appear in the correct conversation.
- Support-sent Chinese text arrives to the user as translated English.
- Sent messages show one check; read messages show two checks on one horizontal line with the timestamp.
- Image and GIF preview do not crop the right edge on desktop or mobile.
- Refreshing either browser does not switch a user account into the support page.
- User registration accepts an invite code and shows the new user's own invite code after login.
- The bottom nav shows the Games entry, and `/pages/games/index` renders the reserved ad/banner area.
