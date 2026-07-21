# MTSR-01 — Android app (WebView wrapper)

This is a complete Android Studio project. It wraps the latest version of
your app (the one with View Data as the default tab) in a native WebView,
so it installs and runs as a real `.apk` — no browser, no third-party app,
fully offline.

## What's inside
- `app/src/main/assets/www/index.html` — your app, unmodified except for
  one addition: "Save JSON" / "Export CSV" now save through a native
  Android bridge into the device's Downloads folder (in a browser this
  wouldn't work reliably, but a plain WebView needs it wired up).
- `app/src/main/java/.../MainActivity.kt` — loads that HTML into a
  full-screen WebView, and implements:
  - the native save bridge above,
  - a file picker for "Load JSON" / "Import CSV" (WebView doesn't support
    `<input type=file>` out of the box; this makes it work),
  - Android back-button = browser back.
- App icon (blue "MTSR" badge) at all standard densities.

## How to build (Android Studio)
1. Open **Android Studio**.
2. **File → Open**, select this folder (the one containing `settings.gradle`).
3. Let Gradle sync (first sync downloads dependencies, needs internet).
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**, or just press ▶ Run
   with a device/emulator connected.
5. The output `.apk` lands in `app/build/outputs/apk/debug/`.

## How to build online for free (GitHub Actions, no Android Studio needed)
This project includes `.github/workflows/build.yml`, which builds the APK
on GitHub's own servers for free.

1. Create a new repository on GitHub (public or private — both get free
   Actions minutes).
2. Upload/push everything in this folder to that repo (keep the folder
   structure as-is, including the hidden `.github` folder).
3. Go to the repo's **Actions** tab. The "Build APK" workflow runs
   automatically on push (or click **Run workflow** to trigger it manually).
4. When it finishes (a few minutes), open the completed run and download
   the **mtsr01-debug-apk** artifact from the "Artifacts" section at the
   bottom of the run page — that's your `.apk`.
5. Unzip the downloaded artifact to get `app-debug.apk`, transfer it to
   your Android phone, and install it (enable "install unknown apps" for
   whatever app you use to open it, e.g. Files or Chrome).

## Notes
- `applicationId` is `com.youngone.mtsr01` — change it in `app/build.gradle`
  if you want a different package name.
- `minSdk 24` (Android 7.0+) covers effectively all active devices.
- For a signed release build (needed to publish or to avoid "unknown
  developer" warnings long-term), use **Build → Generate Signed Bundle/APK**
  and create a keystore when prompted — Android Studio walks you through it.
- Data is stored on-device via `localStorage` inside the WebView, same as
  before; nothing is sent to a server.
