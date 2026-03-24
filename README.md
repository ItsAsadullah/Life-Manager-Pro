<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://github.com/user-attachments/assets/0aa67016-6eaf-458a-adb2-6e31a0763ed6" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/3bb9cdca-6388-4552-8b6c-b41815028708

## Run Locally

**Prerequisites:** Node.js

1. Install dependencies:
   `npm install`
2. Set the `GEMINI_API_KEY` in [.env.local](.env.local) to your Gemini API key
3. Run the app:
   `npm run dev`

## Web Push (FCM) Setup

To enable installed-PWA/background notifications:

1. In Firebase Console, open **Cloud Messaging** and create a **Web Push certificate key pair**.
2. Put the public VAPID key into `.env.local`:
   `VITE_FIREBASE_VAPID_KEY=YOUR_PUBLIC_VAPID_KEY`
3. Ensure Firebase config values are present in `.env.local` (or use `firebase-applet-config.json`).
4. In browser/app settings, allow notifications and enable reminders.

### Backend delivery (production)

This repo now registers device token(s) into `users/{uid}.push.token`.
For true native-like scheduled reminders, add a backend scheduler (Cloud Functions + Cloud Scheduler or another cron worker) that:

- Finds due reminders by time/timezone
- Sends FCM message to stored token
- Includes `title`, `body`, and optional `click_action`

## Android Native Push (Capacitor) - Phase 1

This project is now scaffolded for Android native push with Capacitor.

### One-time setup

1. Download `google-services.json` from Firebase Console for Android app id `com.hisabnikash.app`.
2. Place it at: `android/app/google-services.json`.
3. Build and sync native assets:
   `npm run build:cap`

### VS Code workflow

- Sync Android after code changes: `npm run cap:sync`
- Copy web assets only: `npm run cap:copy`
- Run on connected Android device/emulator: `npm run android:run`

### Token behavior

- Web browser/PWA writes `push.source = web-fcm`
- Native Android app writes `push.source = native-fcm`
- Both store token at `users/{uid}.push.token`
