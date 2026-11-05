# Third-party notices

## External DM100 product photos

The README embeds two external DM100 product photographs from [this public product listing](https://www.ebay.nl/itm/185939246016), hosted at i.ebayimg.com. They illustrate the reference hardware, not PRISME or additional device certifications. They are not original project assets under the MIT licence; rights remain with their respective owners. The photo files are not copied into this repository.

The production app uses Android framework APIs, system fonts and icons read at runtime from applications installed by the end user. No third-party application package, branded watch-face artwork, account data or proprietary firmware is distributed here.

The test harness downloads **com.vaadin.external.google:android-json:0.0.20131108.vaadin1**, the Android JSON implementation, from Maven Central. It is test-only, not bundled into PRISME.apk, and is licensed under Apache License 2.0. The dependency is pinned by SHA-256 in `scripts/fetch-test-dependency.py`.

- [Artifact metadata](https://repo.maven.apache.org/maven2/com/vaadin/external/google/android-json/0.0.20131108.vaadin1/android-json-0.0.20131108.vaadin1.pom)
- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

GitHub Actions and Android/JDK build tools are obtained separately under their own licenses. Names such as Apple, Samsung, Garmin, TAG, Hublot, Montblanc, Windows and macOS in style titles indicate visual inspiration only. No endorsement or affiliation is claimed. Demo icons and the PRISME graphic are original geometric illustrations.

## Optional weather data — PRISME 6

Weather data by [Open-Meteo.com](https://open-meteo.com/), distributed under [Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/). The current forecast and geocoding endpoints are hosted by Open-Meteo. Their [free API terms](https://open-meteo.com/en/terms) permit non-commercial use subject to their limits; commercial redistributors must arrange an appropriate service plan instead of assuming the MIT app licence grants API access. The app requests metric forecasts and optionally converts Celsius to Fahrenheit locally. Icons are drawn by PRISME and are not provider assets.

PRISME identifies the source in the weather detail panel, its settings and documentation. API responses may be cached locally; weather accuracy and availability are controlled by the provider. Published DM100 weather captures use Paris as an explicit demonstration city and show the retrieval time. [Privacy and exact request data](docs/PRIVACY.md).
