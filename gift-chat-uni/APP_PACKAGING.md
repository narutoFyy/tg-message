# CardBrother App Packaging Notes

## Android target

This repository is prepared for Android packaging through the uni-app / HBuilderX workflow.

Current project-level readiness:

- `manifest.json` has a fixed app name and appid
- Production API and WebSocket endpoints are environment-driven
- H5 and App share the same business code
- Networked chat, transactions, uploads, and admin flows use backend APIs instead of local-only mock data

### Build path

1. Open `gift-chat-uni` in HBuilderX.
2. Confirm production API values before packaging:
   - `VITE_API_BASE_URL=/api` for server-hosted H5
   - for direct app builds, switch to your HTTPS API host
3. Use the HBuilderX Android cloud packaging or local packaging flow.
4. Export an APK for direct download distribution.

### Distribution assumption

- This phase targets direct APK download and installation.
- Google Play submission is out of scope for now.

## iOS target

The project now includes the iOS packaging items that can be prepared without an Apple Developer account.

Current readiness:

- Bundle ID: `com.cardbrother.app`
- App display name: `CardBrother`
- iOS minimum target: `12.0`
- iPhone/iPad portrait packaging preset
- iOS icon placeholders in `src/static/app-icons/ios/`
- iOS splash placeholders in `src/static/splash/ios/`
- Camera/photo permission descriptions for `Info.plist`
- Microphone permission description for voice messages
- App Transport Security configured for HTTPS-only production networking
- Production API/WS values are environment-driven

Before cloud packaging or exporting the iOS project:

1. Replace `.env.ios` example hosts with the real production HTTPS/WSS host.
2. Confirm `VITE_API_BASE_URL=https://<your-api-host>/api`.
3. Confirm `VITE_WS_BASE_URL=wss://<your-api-host>/ws`.
4. Replace placeholder icon/splash images with final artwork if available.
5. Open `gift-chat-uni` in HBuilderX and verify `src/manifest.json`.

See `IOS_PACKAGING_PRESET.md` for the full iOS preflight checklist.

Still required later:

- Apple Developer account
- signing certificate
- provisioning profile
- TestFlight / App Store delivery setup
