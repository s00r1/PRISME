# PRISME V6 · Watch-only development

PRISME remains a launcher for **rectangular Android watches**. No smartphone port is planned.

## Release safety

The latest signed public release remains v5.4 until a new APK has been built, tested on the DM100 and signed with the original release key. A PR/branch or an unsigned GitHub Actions artifact is **not** an installable release and must not replace published download buttons.

## Phase 1 foundation

- V7 profile schema, including battery/music and optional future watch modules, with type/range validation before writing.
- Existing v1–v6 JSON imports remain accepted and retain unrelated V6 settings.
- Opt-in contrast on PRISME's **own** dashboard backgrounds, with transparent unfilled areas and reduced per-frame allocations.
- No screenshot capture, notification-reader or screen-reading permission is introduced.
- Android 7.1 remains the minimum. Compatibility with other watches must be tested individually.

## Phase 2 / 3 implementation tracking

Studio, Sense, widgets, dock, and desktop ADB Link are being implemented on the development branch. Features must be verified before advertising them as shipping.

## Builds

See [Build instructions](BUILD.md). Use `./build.ps1 -Unsigned` and run all the tests. Do not disable Play Protect to force an older APK.

## Watch-native modules

- **Studio:** touch-drag positions for four on-watch cards (clock, tasks, timer, device), with independent visibility and size, no image capture.
- **Widgets Engine:** four lightweight cards, refreshed only while the dashboard is visible, shared across the 22 themes.
- **Sense:** optional clock/charge/low-battery style rules and a low-battery animation saver. User must enable it explicitly.

These features require an Android device test before being marketed as stable.

## Universal Edge Dock

A dedicated, opt-in foreground service provides one floating handle and five quick actions over other apps. Overlay permission is mandatory, and reboot activation is separately chosen by the user. It does not bypass OEM background restrictions.

## PRISME Link for desktop

`python scripts/prisme-link.py doctor` checks watch API and screen dimensions. Use `--confirm-watch` to enable `push-profile`, `install` and `open`. Profile import remains manual on the watch; there is no companion phone build and no Internet permission.

## PRISME Doctor / Performance Lab

Doctor reports display geometry, Android API, overlay authorization, service state, battery reader diagnostics, system memory, per-process PSS and free storage. It does not claim a measured frame rate or export device IDs. Widgets throttle telemetry to avoid battery/CPU churn. The backup migration creates a new restore point once when entering V6.

**Security:** The edge dock activation and boot permission remain device-local and are intentionally **not** imported from someone else's theme pack. Imported modules include layout, color and size only.

## Gesture Engine V6 and built-in theme packs

The original upward swipe and empty-space double tap remain backward compatible, with new choices: Studio, Sense, Edge Dock and Doctor. A separately configurable downward swipe is added without overriding the Control Center's edge recognizer or horizontal paging. Studio now ships four watch-native visual packs; they are starting points for user-defined local JSON profiles, not downloaded themes.

## Release process

`.github/workflows/release-6.yml` is intentionally manual. It refuses to publish until an exact main-branch revision has been manually validated on the reference watch, the manifest is marked 6.0.0 (rather than an RC), tests pass, the original signing secrets exist, and the signing certificate matches v5.1. This keeps existing installations update-compatible and avoids distributing an unsigned APK as a final release.

## Desktop Studio (browser, not a phone launcher)

`website/studio.html` and `website/studio.js` provide a self-contained visual editor for the 22 watch themes and four widgets. Load a local V5.4/V6 JSON export (schema 6 or 7), drag widgets, and export a schema-7 profile for watch-side import. Without a real export, the page offers preview only. No profile contents are uploaded or logged.

## Optional signed V6 watch-test artifact

The RC compilation workflow may also create `PRISME-6.0.0-rc1-signed-watch-test` on push to the dedicated V6 branch, but **only if** the repository already has the original three signing secrets. GitHub compares the new APK's signing certificate to the official v5.1 APK and blocks the artifact on mismatch. This is a **test artifact**, not a GitHub Release, and must be tested on a DM100 with Play Protect enabled before any stable publication.
