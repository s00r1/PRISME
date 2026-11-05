# PRISME 6

**A customizable home screen for the DM100 and large rectangular full-Android watches.** Build your pages on the watch or on a PC, keep everyday tools within reach, and keep your data local.

[Official website](https://s00r1.github.io/PRISME/) · [Version 6 release](https://github.com/s00r1/PRISME/releases/tag/v6.0.1) · [PC editor](https://s00r1.github.io/PRISME/home-studio.html)

[Français](README.fr.md) · [English](README.en.md) · [Deutsch](README.de.md) · [中文](README.zh.md) · [Türkçe](README.tr.md) · [Русский](README.ru.md) · [العربية](README.ar.md)

> **Published stable version: 6.0.1 · versionCode 25 · full Android 7.1 / API 25 minimum.** The reference device is a DM100 running Android 7.1.1 at 640 × 480 in landscape. Round/square watches, Wear OS, watchOS and Tizen are not validated targets. No root or firmware replacement is required.

## Downloads

| Official file | Purpose |
| --- | --- |
| [PRISME-6.0.1.apk](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-6.0.1.apk) | Signed Android application; install over the existing PRISME application. |
| [PRISME-PC-Studio-6.0.1.zip](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-PC-Studio-6.0.1.zip) | Complete offline PC editors; extract the ZIP and open index.html. |
| [PRISME-6.0.1-sources.zip](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-6.0.1-sources.zip) | Validated watch application sources, web editors, documentation and tests, packaged as a Release asset. |
| [SHA256SUMS.txt](https://github.com/s00r1/PRISME/releases/download/v6.0.1/SHA256SUMS.txt) | Checksums for the distributed files. |

These files are available in the stable `v6.0.1` Release. Download the [validated V6 sources](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-6.0.1-sources.zip) from the archive attached to that Release. The PC editor also runs [online](https://s00r1.github.io/PRISME/home-studio.html). The offline package needs a modern browser, not a development server or an account. Unsigned workflow artifacts are for developers; use the signed Release APK for installation.

## See PRISME 6

| Modular home | Paper palette |
| --- | --- |
| ![PRISME 6 on DM100](../assets/device/v6/home-graphite.png) | ![Paper palette](../assets/device/v6/home-paper.png) |

| Media page | On-watch editor |
| --- | --- |
| ![Media page](../assets/device/v6/page-media.png) | ![Touch editor](../assets/device/v6/editor.png) |

These are real DM100 screenshots of the delivered V6 interface, in French. The classic-style gallery uses a fictional demonstration catalog rather than personal installed applications.

| Weather and forecast | Widgets for all 22 styles |
| --- | --- |
| ![PRISME 6 weather on DM100](../assets/device/v6/weather.png) | ![Widget catalogue and settings](../assets/device/v6/widgets.png) |

| iOS Control Center | Android Control Center |
| --- | --- |
| ![iOS modules](../assets/device/v6/control-center-ios.png) | ![Android quick settings](../assets/device/v6/control-center-android.png) |

## The selected classic desktop stays primary

Your chosen style remains visible by default. The fixed **22 styles** button in Settings opens the searchable selector and its previews. Choosing a style closes cards and shows that desktop immediately. Cards are an explicit optional panel under Tools; its **Bureau** button and Android Back return to the selected desktop. Importing a card layout keeps the current desktop/card mode unchanged.

## Thirteen widgets for every desktop

**Weather, Clock, Music, Battery, Tasks, Notes, Timers, Calendar, World clock, System, Focus, Shortcuts and Stopwatch** share a dedicated area beside the selected desktop. Add up to **16 instances**, remove, disable or reorder them under **Settings → Widgets**. Weather, Music and Tasks are initially enabled; the other types remain available. Tap to open an instrument and long-press to customize it. Removing a widget keeps its tool data.

Every instance has **six skins** (Desktop signature, Glass, Solid card, Outline, Minimal, Neon), three densities, four presentations, desktop or custom accent, background opacity, corner radius, text scaling, custom title and optional details. Choose which of the 22 styles show it. Place the scrollable area left/right at 144–208 px in landscape, use its upper strip in portrait or hide it. Clocks support digital/analogue/large/date-time displays, seconds, 12/24-hour format and world timezones.

## Optional, integrated weather

Weather offers current conditions and trend, compact display, five days or upcoming hours. Tap for apparent temperature, humidity, wind, twelve hourly entries and five daily entries. Choose °C/°F, manual refresh and a 15–180 minute foreground interval. Cached readings show timestamps and stale/error states; offline data is not represented as a fresh observation.

Under **Settings → Widgets → City and connected weather**, explicitly enable Open-Meteo and manually select a city. No GPS permission is used. The provider receives the IP address and city query or selected city coordinates. City, coordinates, cache and consent stay device-local and never enter imported/exported profiles. Only temperature units and interval are portable. Open-Meteo’s free API is for non-commercial use; attribution and terms are linked in [Privacy](PRIVACY.md).

## Two distinct Control Centers

**iOS** uses separate modules, circular switches and vertical brightness/volume sliders. **Android** uses larger quick-setting tiles, textual states and horizontal sliders. Shortcut selection/order, colors, dimensions, gestures, rotation and media access remain customizable. Android restrictions may require a control to open system settings rather than toggle directly.

## Optional cards and appearance

Start with Main, Work and Media pages. Rename or remove them, or create up to **five pages**. Each page supports up to **eight cards** on a **4 × 4 grid**. Clock, Tasks, Timer, Music, Notes, Favorites and Watch cards can be moved and resized; overlaps and out-of-bounds positions are rejected.

Tap a card to open its function. Long-press for numeric position/size controls. Choose **Edit → Drag the cards**, move a card and select **Done** to leave drag mode. Undo/redo keeps one previous layout state. Notes and tasks are shared across pages rather than duplicated; the home cards do not expose note contents.

Six original palettes are included: **Graphite, Glass, Paper, Instrument, Neon and Minimal**, with a large-text option. The **22 classic desktops** remain available under **Settings → 22 styles**. Open optional cards from **Tools → Desktop and cards**, and use their **Bureau** button to return. Classic desktop styles and card palettes are independent.

## Everyday tools

| Module | What you can do | Practical limit |
| --- | --- | --- |
| Named timers | Create up to eight independent countdowns; pause, resume, restart and delete. | Initial duration: 1–1,440 minutes. Android/OEM power management can delay delivery. |
| Music | Simple media keys, or active session title/artist and player selection. | Rich access is optional; available metadata and actions depend on the player. |
| Calculator | Four decimal arithmetic operations. | Bounded numbers, no expression interpreter or executable scripts. |
| Offline converter | Length, mass, temperature and duration. | Display rounding applies. |
| Notes and tasks | Local autosaved notes, checklist, search and home-card shortcuts. | Plain JSON exports may include this personal content. |
| Time tools | Lap stopwatch, concentration countdown and desk clock. | Keeping the screen awake is an explicit option. |
| Control Center | iOS/Android layouts, shortcut order, appearance, gestures and rotation. | Some controls open Android settings instead of directly toggling a setting. |
| Volume and battery | Floating volume; 24 battery designs, 12 palettes, positioning and diagnostics. | Battery values come from the firmware, not an independent electrical measurement. |
| Studio, Sense, Dock | Classic widgets, local time/charge style rules, optional floating dock. | System permissions and activation stay device-local. |
| Doctor | Local API/display/service/permission/memory/battery diagnostics. | No telemetry or invented frame-rate measurement. |

PRISME overlays are hidden over the modular home to keep cards clear. They return in other applications according to local settings. The Tools panel also offers a master pause/resume command for overlays.

## Install or update

1. Export your current configuration under **Settings → Profiles** and keep the original file.
2. Download the signed `PRISME-6.0.1.apk` and install it over PRISME. From a PC: `adb install -r PRISME-6.0.1.apk`.
3. Do not uninstall the application for a normal update: the original signing certificate is retained, and local notes, tasks and settings are preserved.
4. Open PRISME and choose it as the Android home application if the firmware allows it. Otherwise it works as a normal application.
5. Grant overlay access separately if you want floating controls. Core home pages do not require notification access.

Older JSON profiles remain supported. V6 exports use **schema 7**, a file-format number rather than an application version. Keep a V5 export for rollback: older applications may reject V6 profiles, and Android may refuse a lower versionCode. PRISME does not automatically uninstall your data to downgrade.

## PC editor: prepare, export, apply

1. Open the online editor, or extract the PC ZIP and open `index.html`. On Windows, `LANCER-EDITEUR.cmd` opens that file in the default browser.
2. Load a **complete JSON export from PRISME**. Without one, preview is available but profile export is disabled.
3. Use **Widgets for 22 styles** to add/customize widgets, or **Optional cards** to prepare pages, dimensions, palettes and large text.
4. Export `PRISME-6-Studio.json` and copy it to the watch.
5. Use **Settings → Profiles → Import configuration → Android file picker**, select the file, review and confirm.

Profile contents remain in browser memory and are not uploaded. Other sections of the loaded profile are retained. All 22 reference previews are included offline. The package retains `classic-studio.html` and its four historical widgets. Weather city, cache and consent are configured only on the watch. [Full PC guide, FR](PC-STUDIO.md).

## Privacy and recovery

PRISME has no account, ads or analytics. Local notes, tasks, favorites and launch history stay on the watch. **Internet permission is declared for optional weather**, disabled until explicit in-app consent and manual city selection. The weather provider does not receive the rest of PRISME’s data. Ordinary JSON exports are plaintext and should not be posted in issues.

Rich music access is disabled by default. It requires Android's sensitive notification-listener authorization, but PRISME uses media sessions without reading or storing notification message bodies. The simple mode does not need this access. Overlay, system-setting, Do Not Disturb and boot behavior have their own purposes and local choices.

Automatic recovery points use app-private storage, with ten automatic points retained. A point is made when entering V6 and on activity startup after 24 hours. Imports are validated before writing and protected by a recovery journal. **Tools → Encrypted backups** offers portable AES-256-GCM files protected by a password of at least ten characters; there is no remote password recovery. Active timer executions and system permissions are not imported from a theme.

After three consecutive uncaught crashes, recovery mode offers retry or classic home with overlays disabled. It can also be opened manually. It does not repair firmware or failing storage. [Privacy details](PRIVACY.md).

## Verification and compatibility

Automated suites cover Java models, PC cards/widgets, gallery behavior and real Edge-browser journeys. DM100 checks include migration, rejected imports, recovery, encryption, alarms, media, navigation and display. The validation report distinguishes model assertions, touch journeys and visual inspections, with results and limits for this delivery.

Other watches/firmware variants, prolonged endurance, a physical reboot during this validation session, every third-party player and Play Protect certification are not claimed. Background playback, alarms, rotation and overlays remain subject to Android and the OEM firmware. Historical UI languages are French, English, German, Simplified Chinese, Turkish, Russian and Arabic; some detailed V6 messages still fall back to French.

## Build, documentation and support

```powershell
python scripts/fetch-test-dependency.py
./tests/run-all.ps1
node tests/home-model.cjs
node tests/dashboard-widget-model.cjs
node tests/website.cjs
./build.ps1 -Unsigned
python scripts/package-pc-editor.py
```

[Build instructions](BUILD.md) · [Device tests](../tests/device/README.md) · [V6 guide, FR](V6-GUIDE.md) · [PC editor guide, FR](PC-STUDIO.md) · [Compatibility](COMPATIBILITY.md) · [Classic styles](STYLES.md) · [Validation](V6-VALIDATION.md) · [Release notes](RELEASE-6.0.1.md) · [Future work](V6-DEVELOPMENT.md).

For a bug report, include the model, Android API, PRISME version, orientation and reproduction steps. Do not post accounts, serials, personal exports or raw device logs. [Report an issue](https://github.com/s00r1/PRISME/issues/new/choose).

[MIT license](../LICENSE). Style brand names indicate visual inspiration, not affiliation or supported hardware. The optional Imou companion is separate and is not required by PRISME.
