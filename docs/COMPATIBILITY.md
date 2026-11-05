# PRISME 6 — Compatibility / DM100 reference

**Reference: a large 2.86-inch rectangular screen, 640 × 480 in landscape (4:3), full Android 7.1+ / API 25+.**

**Round and square watches are outside this release's supported target. Other rectangular sizes and resolutions are not validated.**

Current application version: **6.0.1 / versionCode 25**. The manifest minimum remains API 25; adding optional network weather does not turn this release into a Wear OS application.

PRISME is a conventional Android home/launcher application. Its manifest requires Android API 25 (Android 7.1) or newer. It contains Java bytecode and resources with no bundled native CPU library. That minimum is an installation requirement, not a guarantee of every feature on every firmware.

## Confirmed device

**Domiwear / Domino DM100, Android 7.1.1**, with a reference landscape surface of **640 × 480**. Device tests covered the launcher, styles, settings, local tools, Control Center, volume and global portrait/landscape. Rotation was checked with the native face, system settings, ordinary Android apps and the optional Imou integration. No personal device identifiers or raw device reports are published.

The manufacturer's description identifies the DM100 as a full-Android 7.1.1 device with a 640 × 480 display: [Domino product description](https://www.domino-china.com/news/19.html). The [manufacturer specification sheet](https://p.globalsources.com/IMAGES/PDT/SPEC/097/K1219355097.pdf) also lists Android and the display dimensions. These sources describe the model; they do not certify PRISME on every reseller variant.

## Candidates only — not verified support

- Other watches sold as DM100: confirm the actual firmware rather than relying on the product name.
- Other rectangular full-Android watches matching the 2.86-inch / 640 × 480 reference: unverified, requiring a separate device test.

These are conditions for community testing, **not additional certified models**. Check the actual watch: rectangular shape, display size/resolution, full Android, API level, APK installation, home launcher, overlay permissions and battery management. Style names do not identify compatible watch models.

## Outside the validated target

- Round or square watches, regardless of Android version. Installer acceptance does not establish supported hardware or usable layout.
- Wear OS / Pixel Watch / Wear OS Galaxy Watch: not validated; this APK is not a Wear OS watch-face package or Watch Face Format asset. Android's [Wear OS packaging requirements](https://developer.android.com/training/wearables/packaging) describe a distinct watch-app distribution target. Experimental sideloading is not official PRISME support.
- watchOS, Tizen and proprietary systems without Android APK installation cannot use this ordinary Android APK.
- Android below API 25 does not meet the manifest minimum.

## Firmware-dependent behavior

Some tiles open Android settings instead of directly switching a radio or protected setting. Global orientation depends on overlay policy. Background alarms and floating windows may be stopped by manufacturer battery management. Modern Android versions impose additional restrictions; minimum API 25 is not a claim of full Android 8–16 validation.

A reported physical side-band issue on a DM100 remains unresolved. Changing logical resolution or UI scale did not establish a hardware-width repair. PRISME does not alter display drivers or firmware.

## Report another watch

Use the compatibility issue form with model, Android API, display size and the features you tested. Do not post account details, email addresses, serial numbers, Wi-Fi names, tokens, exported profiles or raw device dumps.

## V6 navigation

The selected classic style remains the entry point. All 22 styles stay accessible; cards are optional and provide a permanent return to the selected desktop. A card-profile import does not switch the active desktop/card mode. Current verification is recorded in [V6-VALIDATION.md](V6-VALIDATION.md).

## Widgets and Control Center layouts

The 22 classic styles share a separate widget area with 13 types: weather, clock, music, battery, tasks, notebook, timers, calendar, world clock, system, focus, shortcuts and stopwatch. Weather, music and tasks are visible in the default layout; up to 16 widget instances can be configured, reordered, removed or restricted to selected styles. Existing classic Studio widgets and the optional page/card system remain available separately.

The widget area reserves space beside the desktop in landscape and above it in portrait. Its width/position and each widget’s appearance, density, color, opacity, rounding, text and title can be adjusted. Removing or hiding widgets preserves the selected desktop; when no widget is visible, the reserved area is released. The layouts target the DM100 reference surfaces of **640 × 480 landscape and 480 × 640 portrait**. Layout support is not a claim that every combination of settings has passed visual testing; consult the validation report for completed device checks.

iOS Control Center uses circular switches, a grouped module, compact media controls and vertical brightness/volume levels. Android uses a large clock, wide Material pills, horizontal levels and a full-width media module. Narrow panels adapt their layout. The selected actions and their order remain the same, with actual Android state and the same firmware restrictions. The media module and sliders can be hidden independently. Accessible range actions are provided for the vertical levels; visible close controls and Android Back remain available.

## Optional weather and connectivity

PRISME 6 declares **INTERNET** for optional [Open-Meteo](https://open-meteo.com/) weather. Weather requests are **disabled by default** and require explicit local opt-in. A city is searched and selected manually; the app declares no GPS/location permission. City search and forecasts use HTTPS to Open-Meteo, which receives the IP address and search text or selected city coordinates. Other profile data, notes and tasks are not sent to this provider.

Temperature, apparent temperature, humidity, wind, upcoming hours and five-day forecasts depend on the provider and a working connection. Celsius/Fahrenheit, a foreground refresh interval and a dated local cache are available. An unavailable or old result is labelled accordingly. Disabling weather stops future requests and retains the cache. Network consent, the chosen city and its coordinates remain local to the watch and are not activated by importing a theme.

Older firmware may fail an HTTPS request because of its certificate store, clock or network configuration. PRISME retains the cache and reports failure; it does not bypass certificate verification or fabricate live weather. Weather availability does not certify forecasts, continuous background updates or all regional network conditions.
