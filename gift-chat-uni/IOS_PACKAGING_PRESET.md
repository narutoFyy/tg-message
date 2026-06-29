# CardBrother iOS Packaging Preset

This file tracks iOS packaging settings that can be prepared before an Apple Developer account is available.

## App Identity

- App name: `CardBrother`
- Bundle ID: `com.cardbrother.app`
- uni-app appid: `__UNI__CARDBROTHER85`
- Version: `1.0.0`
- Build number: `100`
- Minimum iOS target: `12.0`
- Devices: iPhone and iPad
- Orientation: portrait

## Icons And Splash

The iOS icon set is under `src/static/app-icons/ios/`.
The iOS splash images are under `src/static/splash/ios/`.

Current images are generated from the existing `src/static/cardbrother-logo.png` and are ready as packaging placeholders. Replace the files with final design assets using the same names when the production icon and launch screen are finalized.

## Network

iOS App Transport Security is set to disallow arbitrary HTTP loads.

Use HTTPS/WSS production endpoints for app builds:

```ini
VITE_API_BASE_URL=https://api.cardbrother.example/api
VITE_WS_BASE_URL=wss://api.cardbrother.example/ws
```

Replace `api.cardbrother.example` with the real production host before cloud packaging or exporting an iOS project.

If the app is built as server-hosted H5, relative paths can still be used:

```ini
VITE_API_BASE_URL=/api
VITE_WS_BASE_URL=/ws
```

## HBuilderX / uni-app

Open `gift-chat-uni` in HBuilderX and confirm:

- `manifest.json` app name is `CardBrother`
- iOS bundle identifier is `com.cardbrother.app`
- iOS icon resources point to `static/app-icons/ios`
- iOS splash resources point to `static/splash/ios`
- Camera and photo permission text is present
- Production API and WS values are HTTPS/WSS before packaging

Signing, provisioning profiles, TestFlight, and App Store delivery remain blocked until an Apple Developer account is available.
