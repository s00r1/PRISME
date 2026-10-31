# Compatibility: tested support versus installation candidates

PRISME is a conventional Android home/launcher application. Its manifest requires Android API 25 (Android 7.1) or newer. It contains Java bytecode and resources with no bundled native CPU library. That minimum is an installation requirement, not a guarantee of every feature on every firmware.

## Confirmed device

**Domiwear / Domino DM100, Android 7.1.1**, with a reference landscape surface of **640 × 480**. Device tests covered the launcher, styles, settings, local tools, Control Center, volume and global portrait/landscape. Rotation was checked with the native face, system settings, ordinary Android apps and the optional Imou integration. No personal device identifiers or raw device reports are published.

The manufacturer's description identifies the DM100 as a full-Android 7.1.1 device with a 640 × 480 display: [Domino product description](https://www.domino-china.com/news/19.html). The [manufacturer specification sheet](https://p.globalsources.com/IMAGES/PDT/SPEC/097/K1219355097.pdf) also lists Android and the display dimensions. These sources describe the model; they do not certify PRISME on every reseller variant.

## Candidates only — not verified support

- Other watches sold as DM100: confirm the actual firmware rather than relying on the product name.
- LEMFO LEM T and LEM16.
- KOSPET PRIME, PRIME SE, PRIME Pro and Optimus 2.
- Other full-Android watches with API 25+ and ordinary APK installation.

These are names for community testing, **not claims that PRISME has been installed or validated on them**. Check the actual watch before trying the APK: full Android versus a proprietary OS, API level, APK installer, available home launcher, display shape, overlay permissions and battery management. Round displays and manufacturer-specific orientation rules require particular attention.

## Outside the validated target

- Wear OS / Pixel Watch / Wear OS Galaxy Watch: not validated; this APK is not a Wear OS watch-face package or Watch Face Format asset. Android's [Wear OS packaging requirements](https://developer.android.com/training/wearables/packaging) describe a distinct watch-app distribution target. Experimental sideloading is not official PRISME support.
- watchOS, Tizen and proprietary systems without Android APK installation cannot use this ordinary Android APK.
- Android below API 25 does not meet the manifest minimum.

## Firmware-dependent behavior

Some tiles open Android settings instead of directly switching a radio or protected setting. Global orientation depends on overlay policy. Background alarms and floating windows may be stopped by manufacturer battery management. Modern Android versions impose additional restrictions; minimum API 25 is not a claim of full Android 8–16 validation.

A reported physical side-band issue on a DM100 remains unresolved. Changing logical resolution or UI scale did not establish a hardware-width repair. PRISME does not alter display drivers or firmware.

## Report another watch

Use the compatibility issue form with model, Android API, display size and the features you tested. Do not post account details, email addresses, serial numbers, Wi-Fi names, tokens, exported profiles or raw device dumps.
