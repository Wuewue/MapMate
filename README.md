# MapMate — Merged Android App

This branch (`merged-android-app`) is the **final integration**: one runnable **native Android app**
(Kotlin + Jetpack Compose) that combines **every team member's real module code** into a single
product, built on our NavX design. Everything lives in **[`mapmate-android/`](mapmate-android/)**.

> This README is only about the merged app on this branch. The original team-lead project overview
> was removed here on purpose — it isn't relevant to this branch. (It's still on `main`.)

---

## 1. How to run

**Fastest — install the prebuilt app (no setup, no keys):**
```bash
adb install mapmate-android/release/mapmate-debug.apk
```
Runs on any Android 7.0+ (API 24) phone or emulator.

**Or build it in Android Studio:**
1. Open the **`mapmate-android/`** folder in Android Studio.
2. Copy `mapmate-android/local.properties.example` → `local.properties` and set `sdk.dir` to your Android SDK path.
3. Let Gradle sync, then **Run** on an emulator or device.

You do **not** need a Google Maps key or a live Firebase project — the map is drawn by the app (a
stylized Compose canvas) and it ships in **demo mode** with placeholder Firebase config + seeded data.
Requirements: Android Studio + JDK 17. minSdk 24, targetSdk 36.

In the demo: any email + any 6‑digit code signs you in. Then tap a friend to set their privacy, tap
**Go** to route to one, open **Settings**, or **Add Friends → accept** to watch someone appear.

---

## 2. What this merges — who built what

Each part is **each member's original logic, kept in Kotlin** — only the package name changed (and
Tue's was ported from JavaScript). Nothing was rewritten.

| Part | Who | What it does in the app | Package |
|---|---|---|---|
| App backbone + home map + Firebase data layer | **Tung** | the app shell, the map screen, the Cloud-Functions data layer | `com.mapmate.ui`, `com.mapmate.data.remote` |
| Profile / login / friends | **Teo & Michael** | sign-in, OTP, friends list, profile, friend requests | `com.mapmate.profile.*` |
| Real-time location + transport | **Mai Nam Khanh** | the Walking / Cycling / Driving mode each friend shows (speed smoother + hysteresis classifier) | `com.mapmate.telemetry.*` |
| Privacy + device + off-grid alerts | **Tran Thanh Dat** | per-friend Precise / Blurred / Frozen on the map, battery state, the "phone battery died" alerts | `com.mapmate.privacy.*`, `com.mapmate.device.*` |
| Distance / nearby | **Vu Tien Tue** | distance to each friend + "within 1 km" (Haversine) | `com.mapmate.distance.*` |

`com.mapmate.demo.MapMateEngine` runs them all together every 1.5s: Khanh classifies movement →
Dat's resolver decides what you're allowed to see → Tue measures distance **from the privacy-filtered
point** → Dat's detector raises off-grid alerts. A blurred or frozen friend never leaks their real
location — not even through the distance number.

---

## 3. What's different from the earlier merge

Our first attempt was a **browser demo** (`MapMate.html`, React/JSX) that *re-implemented* everyone's
logic in JavaScript. It looked like the app but it wasn't Android and wasn't anyone's real code. **This
branch replaces that entirely:**

| Earlier version (web demo) | This branch (merged app) |
|---|---|
| React/JSX, runs in a browser | **Native Android** (Kotlin + Compose), installs as an APK |
| Logic re-written in JavaScript | **Everyone's original Kotlin**, repackaged not rewritten |
| HTML/CSS look-alike | NavX design rebuilt in Compose with the real Syne / DM Sans fonts |
| One page | **12 screens** + working navigation (5-tab nav with the center FAB) |
| Fake data only | A live engine running the **real merged code**; real Firebase path present behind placeholders |

Re-homed during the merge (per our agreed plan): Khanh's telemetry moved **off Realtime Database onto
Firestore**, his protobuf serializer was dropped, and Tue's `distance.js` was **ported 1:1 to Kotlin**.

---

## 4. What's NOT done yet (honest limitations)

- **Not on live Firebase.** Runs in demo mode on seeded data. The real path (Auth + Firestore + Cloud
  Functions) is wired in code but needs a real `google-services.json` (project `mapmate-69a2e`) + the
  functions deployed, then flip `DEMO_MODE` in `mapmate-android/app/build.gradle.kts`.
- **No real GPS / no Google Maps.** The map is a stylized canvas so it runs anywhere with no key. Real
  GPS + a map-tile provider is a later step (the on-device location code is present).
- **A few settings are display-only.** Notifications, Voice guidance and Dark mode toggles hold their
  state but don't drive a backend yet. *Off-grid alerts, Share location, per-friend privacy, and the
  foreground/always-on location setting* **are** wired to real logic.
- **Some screens use placeholder content.** Navigate shows a canned route to the chosen friend; Trips
  are sample trips; Search lists sample places; QR is a stub; Share opens the real Android share sheet.
- **Auth is faked for the demo** (any email + any code). Teo/Michael's real Firebase auth is in the
  code and used in real mode.

---

## 5. Screens

| Map (live merge) | Tap-a-friend privacy | Settings |
|---|---|---|
| ![map](mapmate-android/docs/screenshots/04-home-map.png) | ![privacy](mapmate-android/docs/screenshots/05-tap-friend-privacy.png) | ![settings](mapmate-android/docs/screenshots/11-settings.png) |

All 12 screenshots are in [`mapmate-android/docs/screenshots/`](mapmate-android/docs/screenshots/), and a
deeper write-up (architecture, demo vs real mode) is in
[`mapmate-android/README.md`](mapmate-android/README.md).
