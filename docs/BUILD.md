# Build and test PRISME

The application sources and resources are complete. Private release signing keys and local device data are deliberately absent.

## Windows build

Requirements: PowerShell 7+, JDK 17+, Android SDK Platform 35 and Build Tools 35.0.0 (or compatible installed versions). Set `JAVA_HOME` and `ANDROID_SDK_ROOT`/`ANDROID_HOME`, or pass paths explicitly.

```powershell
sdkmanager "platforms;android-35" "build-tools;35.0.0"
./build.ps1 -Unsigned
```

This creates `PRISME_DM100-unsigned.apk`, a developer artifact that must be signed before installation. Ordinary users should download the signed `PRISME.apk` from Releases. An APK built with a different signing key cannot update the official installation in place.

For a locally signed build, create and keep your own keystore, then set `PRISME_KEYSTORE`, `PRISME_STORE_PASSWORD` and `PRISME_KEY_PASSWORD` in your local environment. Never commit them. Run:

```powershell
./build.ps1 -Keystore $env:PRISME_KEYSTORE -KeyAlias prisme
```

Explicit SDK options are `-SdkPlatform`, `-BuildTools`, `-JavaHome` and `-BuildDirectory`. The APK manifest retains minimum API 25 and target API 28. Compilation against a newer SDK does not make all features work on every newer firmware.

## Tests

Python 3 downloads a pinned test-only JSON implementation from Maven Central, verifies its SHA-256 and stores it in the ignored `tests/libs` directory.

```powershell
python scripts/fetch-test-dependency.py
./tests/run-all.ps1
```

The suites cover preferences and profile compatibility, checklist/search behavior, screen geometry, Control Center gestures, floating-volume geometry and edge-to-edge layout. They do not require a watch or user data. The published sources passed 623 preference/tool assertions, 43,817 viewport assertions, 784 gesture assertions, 20,162 volume-geometry assertions and 109 edge-to-edge assertions.

An optional static API audit requires the SDK's API history file and a built APK:

```powershell
python tests/api25_audit.py --history "$env:ANDROID_SDK_ROOT/platforms/android-35/data/api-versions.xml" --apk PRISME_DM100.apk
```

This checks public framework references and guarded API 26 boundaries. It is not a proof of firmware behavior or a replacement for device testing.

## Optional Imou companion

`companions/imou-landscape` contains Imou Paysage 1.2. Its wrapper accepts the same build options. It maintains landscape only in the supported Imou packages and, on Android 7, yields to PRISME's active global-orientation service. It resumes its normal behavior when PRISME returns to System mode. Cross-app service detection is disabled on API 26+.

The companion is optional and is not required to use PRISME. Its release APK has a separate signing certificate. No Imou application code or account data is included.

## Screenshots

Documentation captures are rendered on an Android device from an isolated demo application using the same UI code and fictional data. Views belonging to other applications are excluded. The temporary demo package and its signing key are not release assets. App-installation lists, backups and real user text are not used as screenshot fixtures.
