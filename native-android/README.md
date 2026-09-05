# Hisab Nikash Native Android

This is the new native Android implementation. It is intentionally independent of the existing React/Capacitor app, so the old app remains usable while feature parity is built.

## Open and run

1. Open the `native-android` folder in Android Studio.
2. Copy the existing Firebase configuration to `native-android/app/google-services.json`.
3. Sync Gradle and run on an Android 7.0+ emulator or device.

For a command-line build on Windows, set `JAVA_HOME` to Android Studio's `jbr` folder and `ANDROID_HOME` to your Android SDK folder, then run `./gradlew.bat :app:assembleDebug`.

The package name is `com.hisabnikash.app`, matching the existing Firebase Android app. Before production sign-in, add the release and debug SHA-1/SHA-256 certificate fingerprints to that Android app in Firebase Console and download a fresh `google-services.json`.

## Migration sequence

1. Firebase Google authentication and auth-gated navigation — implemented
2. Expenses and income (same Firestore collections)
3. Dashboard totals and charts
4. Notes, debts, market memo, and shared memo
5. OCR, voice input, reminders/FCM, and AI endpoints

Do not embed the Gemini API key in the Android app; keep AI calls behind the existing server endpoint.

## Google sign-in setup

Enable the Google provider in Firebase Authentication. Add this app's debug and release SHA-1/SHA-256 fingerprints in Firebase Console, then download the updated `google-services.json`. The app uses Android Credential Manager and Firebase Authentication; the profile document created at `users/{uid}` has the same fields as the existing web app.
