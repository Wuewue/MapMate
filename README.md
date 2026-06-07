# MapMate — Merged Android App (final integration)

> **This branch adds [`mapmate-android/`](mapmate-android/) — one runnable native Android app that
> merges every team member's real module code into a single product, built on our NavX design.**
> Full write-up (screenshots, architecture, who-built-what): **[`mapmate-android/README.md`](mapmate-android/README.md)**.

## What we did

Each of us built a separate piece of MapMate in a separate repo and stack. This branch combines
them into **one Android app** (Kotlin + Jetpack Compose), keeping each person's original logic and
language — nothing rewritten, only package names changed and the storage layer reconciled.

| Module | Owner(s) | Lives in |
|---|---|---|
| Backbone, map UI, Firebase data layer | Tung | `com.mapmate`, `com.mapmate.data.remote`, `com.mapmate.ui` |
| Profile / Auth / Friends | Teo & Michael | `com.mapmate.profile.*` |
| Real-time location / transport classifier | Mai Nam Khanh | `com.mapmate.telemetry.*` |
| Privacy / Device / Off-grid alerts | Tran Thanh Dat | `com.mapmate.privacy.*`, `com.mapmate.device.*` |
| Distance / nearby (ported JS → Kotlin) | Vu Tien Tue | `com.mapmate.distance.*` |

**12 screens, 1:1 with the NavX design** (Splash, Login, OTP, Map, Search, Navigate, Friends,
Add Friend, Trips, Profile, Settings) with the real Syne/DM Sans fonts and the 5-tab nav.
**Working interactions:** per-friend privacy (tap any friend → Precise/Blurred/Frozen), "Go" routes
to a friend, accepting a request adds them, off-grid alerts toggle gates the alert engine, and the
foreground/always-on location setting is wired to the privacy module.

The live demo runs everyone's real code together on seeded data — transport classification (Khanh)
→ privacy resolver (Dat) → distance from the *privacy-filtered* point (Tue) → off-grid alerts (Dat),
so blurred/frozen friends never leak their real position.

## How to run

- **Android Studio:** open `mapmate-android/`, let Gradle sync, Run on an emulator or device.
  (Copy `mapmate-android/local.properties.example` → `local.properties` and point `sdk.dir` at your Android SDK.)
- **Or just install the prebuilt app:** `adb install mapmate-android/release/mapmate-debug.apk`

No Google Maps key and no live Firebase are needed to run — the map is a stylized Compose canvas and
the app ships in demo mode with placeholder Firebase config. The real Firebase path is present for
when keys + Cloud Functions are deployed.

---

# MAPMATE project

## Overview

This is a map based mobile application which working also as a social media. Help users to track, share their and friend's location. Also share the moment where you checked in a place.

### Objective

- Deliver a logical, functional and appealing appearance  application
- Meet the project deadlines and requirements
- Ensure the scability and maintainability

## Scope

### Core feature
- Profile making and all relating(sign in, sign up, add friend,...)
- Share location
- Mark location
- Show they moving status
- SOS 
- Quick Message
- UI/Ux design
- Testing and deployment

# Team & Roles

| Role | Name | Responsibilities |
|------|------|----------------|
| Project Manager | TUng | Planning, coordination, tracking |
| Developers | [...] | Coding, implementation |
| Designer | [...] | UI/UX design |
| QA Tester | [...] | Testing, bug reporting |


## Detailed Features
Fill later
- User Authentication (Login/Register)
- [Feature 1]
- [Feature 2]
- [Feature 3]

---
## Tech Stack

- Frontend: [HTML, CSS, JS / React ]
- Backend: [Node.js / Python /]
- Database: [MySQL / MongoDB / ]
- Tools: Git, GitHub 

---

## Timeline & Milestones

| Phase | Description | Deadline |
|------|------------|----------|
| Planning | Define requirements | 1st week of april |
| Design | UI/UX design | 2nd week of april |
| Development | Build features | 2nd - 4th week of april |
| Testing | Debug & QA | 1st week of may |
| Deployment | Release app | 1st week of may |

---

## Development Process
the process model will be dicuss later

---

## Task Management
All the task will be tracked in githu project so each member when you've done your part please enter the poject and update the process status
---

##  Risk Management

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Delayed development | High | Better planning, buffer time |
| Bugs/errors | Medium | Regular testing |
| Scope creep | High | Strict requirement control |

later
---

##  Communication Plan

- Daily updates: In Message in Discord or GitHub project
- Weekly meetings: Progress review will be hold in Discord
- Documentation(*this is important): please whenever you've done sth write an description and also possible scability, test cases, corresponding supporting features.

---

##  Testing
later
---

##  Deployment
later
---
