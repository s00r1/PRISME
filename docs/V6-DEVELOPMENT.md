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
