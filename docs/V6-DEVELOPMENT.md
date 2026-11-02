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
