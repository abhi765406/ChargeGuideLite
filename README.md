# ChargeGuard Lite

A lightweight Android charging and storage assistant designed for older, low-memory phones. The app uses classic Android Views and no third-party runtime libraries.

## Important limitation

A normal Android app cannot enter Safe Mode, kill/disable arbitrary apps silently, change charging current, or clear another app's data. Android reserves app suspension/hiding and remote data clearing for a Device Owner/Profile Owner (enterprise management), system app, root, or ADB. ChargeGuard Lite stays non-root and safe: it monitors battery state, warns about heat, opens official settings, and identifies potentially unused launcher apps after the user grants Usage Access.

Disabling an app also does **not** erase its installed APK or all of its data. To reclaim substantial space, use the app's system page to Clear storage/cache, or uninstall it. Clearing storage removes that app's local data and sign-in.

## Compatibility

- Minimum: Android 5.0 / API 21
- Target/compile: Android 15 / API 35
- Suitable for Android 8.1-era phones such as Vivo Y91-class devices
- No ads, analytics, network permission, or background service

Manufacturer settings vary. On Funtouch OS, Battery and App Manager screens may use different labels.

## Features

- Live battery percentage, charging state/source, and temperature
- Heat warning at 38°C and high-temperature warning at 42°C
- Notification when power is connected
- Shortcuts to Battery Saver, Battery Usage, Developer Options, and Storage settings
- 30-day unused-app scan using Android Usage Access
- One-tap opening of each app's official App info screen for Disable, Force stop, Clear cache, or Clear storage
- Low-memory UI with no third-party dependencies

## Build with GitHub Actions

1. Create an empty GitHub repository.
2. Upload **the contents inside this folder**, including the hidden `.github` folder.
3. Commit to `main`.
4. Open **Actions** > **Build Android APK** > **Run workflow**.
5. Open the completed run and download `ChargeGuardLite-debug-apk` from **Artifacts**.
6. Extract `app-debug.apk`, copy it to the phone, and install it. Allow installation from that source if Android asks.

The workflow installs Java 17, Gradle 8.9, Android SDK 35, and Build Tools 35.0.0, then runs `assembleDebug`.

## Local build

```bash
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## First use

1. Open ChargeGuard Lite and allow notifications if requested.
2. Tap **Grant Usage Access**, enable ChargeGuard Lite, and return.
3. Tap **Scan apps unused for 30+ days**.
4. Tap an app and use only the system actions you understand.
5. Before charging, use Battery Saver and restrict suspicious apps from Android's Battery Usage screen.

## Troubleshooting GitHub builds

- Ensure `.github/workflows/build-apk.yml` was uploaded. GitHub's web uploader can hide dot-folders if you upload files incorrectly.
- Keep the repository root layout unchanged: `app`, `.github`, `build.gradle`, `settings.gradle`, and `gradle.properties` must be at the top level.
- The first build can take several minutes because Gradle and Android components are downloaded.
