# Privacy and permissions

PRISME works locally. Its manifest has no `android.permission.INTERNET`; no accounts, advertising, analytics SDK or telemetry endpoint are included.

Notes, checklist items, favorites, custom labels, profiles and the history of app launches made from PRISME are stored in the app's local preferences/files. PRISME reads launchable application names/icons to build the user's launcher. Its manual exports can contain personal settings and local content, so they should not be posted publicly. Uninstalling may remove local data. The application disables Android's automatic app backup in the manifest.

## Declared permissions

| Permission | Purpose |
|---|---|
| ACCESS_NETWORK_STATE / ACCESS_WIFI_STATE | Display local connectivity status |
| CHANGE_WIFI_STATE | Wi-Fi control on Android versions that allow it |
| BLUETOOTH / BLUETOOTH_ADMIN | Bluetooth status/control on supported Android versions |
| MODIFY_AUDIO_SETTINGS | Local volume and audio controls |
| VIBRATE | Optional touch feedback and timer alert |
| WRITE_SETTINGS | User-authorized system-setting controls where supported |
| ACCESS_NOTIFICATION_POLICY | Optional sound/Do Not Disturb control with Android authorization |
| SYSTEM_ALERT_WINDOW | Floating volume and global orientation |
| FOREGROUND_SERVICE | Keep user-enabled floating/orientation services visible to Android |
| RECEIVE_BOOT_COMPLETED | User-enabled restoration and timer rescheduling |

No contact, account, message, camera, microphone or location-reading permission is requested by PRISME. A shortcut may open another application or a system setting with its own permissions. Network-state permissions do not grant Internet access.

The optional Imou helper declares overlay, usage-access, foreground-service and boot permissions. It uses foreground-app events locally to apply orientation only to Imou; it does not read Imou accounts or camera video. See its source separately.

## Public-repository hygiene

Private keystores, signing passwords, local backup archives, exports, raw watch logs, serials, network names and personal machine paths are excluded. Public build scripts accept local SDK/JDK paths and signing secrets through arguments/environment variables. Commits use a generic project identity. The public GitHub account/repository name remains public as part of GitHub hosting.

Screenshots use an isolated demo build with fictional apps, generic icons, sample tasks and sample notes. They capture PRISME's own views rather than another app's windows. No account screenshots or real installed-app catalog are used. APK signing certificates contain a public verification key; the private signing keys are never distributed.

## V6 beta changes

The optional MediaAccess service is protected by Android's BIND_NOTIFICATION_LISTENER_SERVICE permission. The user must separately opt in inside PRISME and grant access in Android. PRISME uses MediaSessionManager for active media metadata and transport controls; notification message bodies are not read or stored. Device-local media consent is excluded from imported profiles.

Automatic recovery files and the restore journal are held in app-private storage. Ordinary JSON exports are plaintext. Optional portable vaults use PBKDF2-HMAC-SHA1 (120,000 iterations, random 16-byte salt), a 256-bit AES key, AES-GCM with a random 12-byte nonce and a 128-bit authentication tag. SHA1 here is the PBKDF2 PRF required for Android 7 compatibility, not a file integrity checksum. Password derivation runs outside the UI thread. Files cannot be recovered without the password. Local filesystem encryption and hardware-backed key storage are not assumed.

V6 screenshots are real DM100 captures of configuration screens and generic home cards. Notes and installed-app catalogs are not exposed in these images. Media titles from a synthetic local QA session are not user listening history. No new Internet permission is declared.
