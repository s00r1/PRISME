# Privacy and permissions

PRISME has no account, advertising, analytics SDK or telemetry endpoint. Notes, checklist items, favorites, labels, profiles and the history of app launches made from PRISME stay in app-local preferences/files. Application names and icons are read to build the launcher. Manual JSON exports may include personal content and should not be posted publicly. Uninstalling may remove local data; Android automatic app backup is disabled in the manifest.

## Optional connected weather in version 6

Version 6 declares **android.permission.INTERNET** for weather. Weather requests are disabled until the user explicitly enables connected weather inside PRISME and chooses a city manually. No GPS/location permission, account or location tracking is used.

The selected provider is Open-Meteo:

| Request | Data sent to the provider |
| --- | --- |
| City search, `geocoding-api.open-meteo.com` over HTTPS | The city search text, language, and normal connection information including IP address. |
| Forecast, `api.open-meteo.com` over HTTPS | Coordinates of the manually selected city, requested forecast fields, and normal connection information including IP address. |

PRISME does not send notes, tasks, app catalogs, profile contents, media titles or identifiers to the weather provider. Requests use two fixed HTTPS hosts, reject redirects, bound response sizes and use timeouts. They run outside the UI thread. Refresh is requested while the weather UI/widgets are in use; no repeating background weather service or alarm is scheduled. Provider availability and Android connectivity remain necessary.

The city, its coordinates/timezone, network consent and forecast cache stay device-local and are excluded from ordinary profiles and portable vault payloads. **Only Fahrenheit/Celsius and the foreground refresh interval are portable**, in `.v6.weather`; widget appearance travels separately in `.v6.dashboardWidgets`. Importing either cannot enable networking or select a location. PC Studio uses fictional weather and never contacts the provider.

Cached forecasts carry their retrieval timestamp and stale/error indications. Disabling connected weather prevents new requests and leaves the local cache available; it does not withdraw information already sent to the provider. A response arriving after a disable/city change is discarded for that configuration. Information already transmitted is subject to the provider’s policies.

Attribution: **Weather data by Open-Meteo.com**. The free API is for non-commercial use under its service terms; the weather data requires attribution under CC BY 4.0. See [Open-Meteo terms](https://open-meteo.com/en/terms), [forecast documentation](https://open-meteo.com/en/docs) and [geocoding documentation](https://open-meteo.com/en/docs/geocoding-api). Commercial redistribution must review the applicable provider terms separately from PRISME’s MIT code license.

## Declared permissions

| Permission | Purpose |
| --- | --- |
| INTERNET | Optional, explicitly enabled HTTPS weather search/forecasts. |
| ACCESS_NETWORK_STATE / ACCESS_WIFI_STATE | Local connectivity status. |
| CHANGE_WIFI_STATE | Wi-Fi controls on Android versions that allow them. |
| BLUETOOTH / BLUETOOTH_ADMIN | Bluetooth status/control where supported. |
| MODIFY_AUDIO_SETTINGS | Local volume and audio controls. |
| VIBRATE | Optional touch feedback and timer alerts. |
| WRITE_SETTINGS | Separately authorized system-setting controls. |
| ACCESS_NOTIFICATION_POLICY | Optional sound/Do Not Disturb controls with Android authorization. |
| SYSTEM_ALERT_WINDOW | Floating controls and global orientation. |
| FOREGROUND_SERVICE | Keep user-enabled floating/orientation services visible to Android. |
| RECEIVE_BOOT_COMPLETED | Locally enabled restoration and timer rescheduling. |

PRISME does not request contacts, account, message, camera, microphone or location-reading permissions. A shortcut may open another application/system setting governed by its own permissions.

The optional Imou helper separately declares overlay, usage-access, foreground-service and boot permissions. It uses foreground-app events locally to apply orientation to Imou, without reading Imou accounts or camera video. It is not required by PRISME; see its source separately.

## Music and local activation

The optional MediaAccess service is protected by Android’s `BIND_NOTIFICATION_LISTENER_SERVICE` permission. The user must opt in inside PRISME and grant Android notification-listener access separately. PRISME uses active media metadata and transport controls; it does not read or store notification message bodies. The simple media-key mode needs no notification-listener access. Device-local consent is excluded from imported profiles.

The 6.0.1 music correction reads title, artist, album and cover art from the selected authorized media session. A provider's alternate display fields are used when its main fields are blank. Notification text is not used as a metadata fallback. Cover images are kept only in a bounded in-memory cache, scaled to at most 256 pixels on the longest edge; they are not saved to disk, exported, logged or uploaded. Images may be supplied as session bitmaps or readable local `content://` / `android.resource://` resources. HTTP/HTTPS, arbitrary file paths and other URI schemes are refused. A local provider may still deny access, and a player may not supply art at all.

Track changes invalidate the old cover before a new image is decoded. Disabling enriched mode or losing its Android grant clears the session snapshot and artwork. A diagnostic access state distinguishes the PRISME opt-in from Android's grant; imports cannot grant either one. Enabling notification-listener access is an explicit user action in Android, never an automatic repair.

Desktop/card mode, automatic Sense activation and sensitive permissions remain local during import. A profile cannot force the optional card panel over the selected classic desktop. Public launcher commands open approved screens; sensitive Control Center commands require a process-local internal token.

## Recovery and encryption

Recovery files and the restore journal are kept in app-private storage. Ordinary JSON exports are plaintext. Optional portable vaults use PBKDF2-HMAC-SHA1 (120,000 iterations, random 16-byte salt), a 256-bit AES key, AES-GCM with a random 12-byte nonce and 128-bit authentication tag. SHA1 is the PBKDF2 PRF used for Android 7 compatibility, not a file integrity checksum. Password derivation runs outside the UI thread. The password is required to recover a vault; no remote recovery is offered. Device filesystem encryption or hardware-backed key storage is not assumed.

## PC editor and public-repository hygiene

Online PC Studio loads its public HTML/scripts/reference images from the website but keeps imported profile content in browser memory, without uploading it. The offline ZIP includes those dependencies and works directly from `file://`. Closing the tab loses changes that were not exported. Editor screenshots and sample weather values are fictitious and labelled as such.

Private keystores, signing passwords, local backups/exports, raw watch logs, serials, network names and personal machine paths are excluded from the public repository. Build scripts accept machine paths and signing secrets through local arguments/environment variables. Public APK signing certificates contain a verification key, never the private signing key.

Published screenshots use PRISME’s own views, fictional catalogs or generic tools. Private notes and real installed-app catalogs are not shown. Synthetic media-session titles are not listening history. A weather screenshot may show a deliberately selected public city and provider data; it is not a GPS location capture.
