# PRISME 5.1

[Français](README.fr.md) · [English](README.en.md) · [Deutsch](README.de.md) · [中文](README.zh.md) · [Türkçe](README.tr.md) · [Русский](README.ru.md) · [العربية](README.ar.md)

**A customizable launcher for full-Android smartwatches.** Offline, with no account, advertising or telemetry.

## Install

**[Download the ready-to-install PRISME.apk](https://github.com/s00r1/PRISME/releases/latest/download/PRISME.apk)** · [Releases and checksums](https://github.com/s00r1/PRISME/releases)

The watch needs **full Android 7.1 or later, API 25 minimum**, and ordinary Android APK installation. Download the APK in the watch browser, open it in Android’s installer and allow that installation source if prompted. Launch PRISME and select it as your home app if the firmware allows; it also opens as a normal app. Allow display over other apps for floating volume and global orientation. Choose **Settings → Style → Language**. Install official updates over the existing app.

Computer installation: `adb install -r PRISME.apk`. No root, firmware replacement or Google-service modifications are required.

## Features

- **22 distinct layouts**: calendar, desktop, app gallery, terminal, arcade, constellation, typography, mosaic, bubbles, instruments, notebook and minimal views.
- Persistent native-watch-face, Styles, Today and Settings navigation. Hold Today for five customizable shortcuts.
- **iOS-style or Android-style Control Center** with configurable tiles, order, colors, transparency, size, position and gestures. Some commands open Android settings when direct control is restricted.
- **Portrait / Landscape**: tap to rotate, hold for reversed orientations or System mode. Notification actions work from other apps. Verified on DM100; other firmware may restrict this feature.
- **Floating volume button** with multiple styles/shapes, size, opacity, position, expandable controls and optional idle fading.
- App/settings search, favorites, recent apps, workspaces and custom labels.
- Today, autosaving notes, checklists, Android-alarm timer, lap stopwatch and desk clock.
- Overall scale, margins, text/icon size, colors, profiles and local safety backups.
- **French, English, German, Simplified Chinese, Turkish, Russian and Arabic**, including right-to-left presentation.

## Watches

| Model or family | Status |
|---|---|
| Domiwear / Domino DM100, Android 7.1.1 | **Device-tested**, landscape reference 640 × 480 |
| Other DM100 variants | Verify individually; similar names do not prove identical firmware |
| LEMFO LEM T / LEM16 | **Untested** community-test candidates if the Android requirements are met |
| KOSPET PRIME / PRIME SE / PRIME Pro / Optimus 2 | **Untested** under the same conditions; round-screen layout and global rotation are not guaranteed |
| Other full-Android API 25+ watches | Installation and features need firmware-specific evaluation |

Untested models are **candidates, not a certified support list**. Verify the actual OS/API and APK installation support. This is not a Wear OS watch-face package. Pixel Watch and Wear OS Galaxy Watch models are not validated. watchOS, Tizen and proprietary systems without Android APKs are outside the target. [Requirements and sources](COMPATIBILITY.md).

## Screenshots, privacy and limitations

[Menus and options](SCREENSHOTS.md) · [All 22 styles](STYLES.md)

Images show the real interfaces of an isolated demo build with fictional data and generic icons, not a user account or installed-app list. The app declares no Internet permission. Notes, tasks, favorites and launches made from PRISME stay locally; user-requested exports can contain that information. No private keys, user backups, account screenshots, watch identifiers or personal logs are published.

Manufacturer battery policies can stop timers and floating services. Music controls need an active player. Secondary translations may need refinement. Physical side bands reported on a DM100 have **no confirmed fix**; PRISME does not alter the panel or repair display drivers. Layout on other screens is not guaranteed.

Java source, resources, language catalogs, tests and the optional Imou companion are included. [Build and test](BUILD.md) · [Permissions](PRIVACY.md) · [Report compatibility](https://github.com/s00r1/PRISME/issues/new/choose). Source is published for inspection; no open-source licence has been selected yet. The APK may be downloaded, installed and used free of charge. Brand names describe visual inspiration, without affiliation.

![PRISME — en](../assets/screenshots/language-en.png)
