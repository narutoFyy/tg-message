# Xcard App Packaging Notes

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

### Startup checks

- Account restoration has an 8-second request deadline. If the network stalls, the login form appears with a connection message while keeping the stored session.
- Native push initialization is disabled by default. Set `VITE_APP_NATIVE_PUSH_ENABLED=true` only after adding and configuring the native Push module in the APK. Otherwise HTML5+ displays a missing-module dialog at startup.
- Verify a cold launch with an existing session while offline, as well as a fresh login, before distributing an APK.

### Google Play preparation

- The source release is now 1.0.6 / versionCode106 with minimum SDK24 (Android 7.0+) and target SDK36. The explicit minimum fixes the Google Play automatic-protection rejection of the earlier minSDK21 bundle; verify both SDK values in every final AAB.
- Android `excludePermissions` removes package installation, package enumeration, broad storage/media access, and phone-state permissions during cloud packaging. Inspect the final AAB manifest to verify the removal.
- All image/video upload entry points use `src/utils/systemMedia.ts`. Android uses the system Photo Picker (or document picker on older devices), copies only selected files to private cache, and retains a separate camera action. H5/iOS retain their existing media APIs.
- Type checking and App/H5 resource builds do not generate a signed AAB. Select Google Play / AAB in HBuilderX Android cloud packaging and keep the original `com.cardbrother.app` signing identity.
- Verify target36 support, native-library 16 KB compatibility, final permissions, and signed-bundle installation before submission. Public privacy/deletion links, operator details, review credentials, and distribution countries still need completion.

### Privacy consent and account deletion

- `npm run build:app` and `build:h5` first run `scripts/build-legal.mjs`. The shared document source is `src/data/legal.json`; the script generates `src/androidPrivacy.json` and offline HTML documents under `src/static/legal/`.
- The Android native privacy template must be included in the new cloud package. Its refusal flow exits before starting the app runtime. Verify this on a fresh installation of the final APK/AAB; replacing H5 assets alone does not add the native template or remove Android permissions.
- H5/iOS use the in-app first-use dialog. Login, registration and admin login require a separate unchecked agreement checkbox. Business API calls, uploads, sockets and push initialization wait for privacy consent.
- Account deletion is under Settings > Delete account for ordinary users. It requires the current password and an explicit signed-in Bearer token. The backend must include Flyway V45 before this frontend is deployed.
- Deletion closes the account, scrubs the core profile and saved payee fields, disables its invite code, removes push bindings, and revokes access. Messages, uploaded media and transaction/audit history are retained as disclosed in the confirmation and policy; this is not a purge of every historical record.
- The policy's operator, contact email and retention schedule remain blank pending business information. Complete those fields and review the retained-data purposes before store submission. Once deployed, the bundled public document path is `/static/legal/privacy.html`; its public availability has not yet been verified.

## iOS target

The project now includes the iOS packaging items that can be prepared without an Apple Developer account.

Current readiness:

- Bundle ID: `com.cardbrother.app`
- App display name: `Xcard`
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
