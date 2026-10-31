# Release validation

PRISME 5.1, version code 15; minimum Android API 25, target API 28.

- Windows source build completed using the public portable script.
- 623 preference/tool assertions passed.
- 43,817 viewport assertions across 378 configurations passed.
- 784 Control Center gesture assertions passed.
- 20,162 floating-volume geometry assertions across 720 configurations passed.
- 109 edge-to-edge assertions passed.
- Static API 25 public-reference checks found no incompatible or unresolved production references at release validation; guarded API 26 helpers remain isolated.
- The release APK's v1/v2 signatures were verified. It is the signed production artifact, not the unsigned developer build or the demo application.
- 32 UI captures and 22 style previews use fictional data in an isolated Android demo build. PNG files contain no text or EXIF metadata.

Tests are scoped to the code and device documented in the compatibility page. They are not a claim of universal smartwatch compatibility.
