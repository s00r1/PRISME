# PRISME 6

Ein anpassbarer Startbildschirm für die DM100 und große rechteckige Uhren mit vollständigem Android. Gestalte Seiten auf der Uhr oder am PC; Notizen, Aufgaben und Einstellungen bleiben lokal.

[Français](README.fr.md) · [English](README.en.md) · [Deutsch](README.de.md) · [中文](README.zh.md) · [Türkçe](README.tr.md) · [Русский](README.ru.md) · [العربية](README.ar.md)

> Aktuell: **6.0.1 · versionCode 25 · Android 7.1 / API 25 mindestens**. Referenzgerät: DM100, Android 7.1.1, 640 × 480 im Querformat. Runde/quadratische Uhren, Wear OS, watchOS und Tizen sind keine validierten Ziele. Root und ein Firmwarewechsel sind nicht erforderlich.

## Downloads

| Downloads | PRISME 6 |
| --- | --- |
| [PRISME-6.0.1.apk](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-6.0.1.apk) | Signierte Android-App für die Uhr. Über die vorhandene Installation installieren. |
| [PRISME-PC-Studio-6.0.1.zip](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-PC-Studio-6.0.1.zip) | Vollständige PC-Editoren für die Offline-Nutzung. ZIP entpacken und index.html öffnen. |
| [PRISME-6.0.1-sources.zip](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-6.0.1-sources.zip) | Quellcode, Tests, Web-Editoren und Dokumentation. |
| [SHA256SUMS.txt](https://github.com/s00r1/PRISME/releases/download/v6.0.1/SHA256SUMS.txt) | Prüfsummen der veröffentlichten Dateien. |

[PRISME · Web](https://s00r1.github.io/PRISME/) · [PC Studio · Web](https://s00r1.github.io/PRISME/home-studio.html) · [GitHub · v6.0.1](https://github.com/s00r1/PRISME/releases/tag/v6.0.1)

## Echte Ansichten der V6

| PRISME 6 | PRISME 6 |
| --- | --- |
| ![DM100](../assets/device/v6/home-graphite.png) | ![DM100](../assets/device/v6/home-paper.png) |
| ![DM100](../assets/device/v6/page-media.png) | ![DM100](../assets/device/v6/editor.png) |

Echte DM100-Aufnahmen der V6-Oberfläche auf Französisch. Die klassische Galerie verwendet fiktive Demodaten, keine persönliche App-Liste.

## Seiten und Gestaltung

**Der ausgewählte klassische Desktop bleibt standardmäßig sichtbar.** Die feste Taste „22 styles“ in den Einstellungen öffnet alle Stile. Karten sind optional; „Bureau“ und Android Zurück führen zum ausgewählten Desktop. Eine Stilwahl zeigt diesen sofort an.

Drei Seiten sind vorbereitet: Hauptseite, Arbeit und Medien. Bis zu **fünf Seiten**, **acht Karten je Seite** und ein **4 × 4-Raster** sind möglich. Uhr, Aufgaben, Timer, Musik, Notizen, Favoriten und Gerätestatus lassen sich verschieben und in ihrer Größe ändern. Überlappungen und Positionen außerhalb des Rasters werden abgewiesen.

Antippen öffnet die Funktion; langes Drücken öffnet Position und Größe. Unter Bearbeiten den Ziehmodus wählen, eine Karte verschieben und mit Fertig beenden. Rückgängig/Wiederherstellen hält einen vorherigen Zustand bereit. Die Seiten teilen sich dieselben Notizen und Aufgaben. Notiztexte werden auf den Startkarten nicht offengelegt.

Sechs Paletten: Graphit, Glas, Papier, Instrument, Neon und Minimal; große Schrift ist optional. Die **22 klassischen Desktops** bleiben erhalten. Unter Bearbeiten zum klassischen Desktop wechseln; über die Suche „modularer Startbildschirm“ zurückkehren. Beide Gestaltungen sind voneinander unabhängig.

## Widgets auf allen 22 Desktops

Zusätzlich zu den optionalen Rasterseiten gibt es einen gemeinsamen Widget-Bereich für die **22 klassischen Stile**. Der Katalog enthält **13 Typen**: Wetter, Uhr, Musik, Akku, Aufgaben, Notizbuch, Timer, Kalender, Weltuhr, System, Konzentration, Verknüpfungen und Stoppuhr. Wetter, Musik und Aufgaben sind in der Standardbelegung sichtbar; weitere Widgets lassen sich hinzufügen, entfernen oder neu ordnen. Bis zu 16 Einträge sind möglich. Entfernen oder Ausblenden eines Widgets ändert den ausgewählten Desktop nicht.

Unter **Einstellungen → Widgets** oder mit langem Druck auf ein Widget lassen sich Darstellung, Dichte, Akzent, Transparenz, Rundung, Schriftgröße, Titel und Sichtbarkeit je Stil einstellen. Es gibt sechs Oberflächen: Desktop-Signatur, Glas, Vollfläche, Kontur, Minimal und Neon. Die Uhr bietet auch eine analoge Ansicht; die Weltuhr hat eine wählbare Zeitzone. Im Querformat steht der Bereich links oder rechts, im Hochformat oben; der Desktop behält einen eigenen Bereich. Der Bereich kann ganz ausgeblendet werden. Die letzte Änderung lässt sich zurücknehmen und wiederherstellen.

### Optionales Wetter mit Open-Meteo

Die verbundene Wetterfunktion ist **standardmäßig deaktiviert**. Erst in den Wettereinstellungen aktivieren, dann eine Stadt manuell suchen und wählen; GPS und Standortberechtigung werden nicht benötigt. Open-Meteo erhält dabei die IP-Adresse sowie den Suchtext oder die Koordinaten der gewählten Stadt. Aktuelle Temperatur, gefühlte Temperatur, Feuchtigkeit, Wind, die nächsten Stunden und fünf Tage werden im Detailfenster angezeigt. Celsius/Fahrenheit und das Aktualisierungsintervall im Vordergrund sind einstellbar. Datum und Alter des lokalen Caches bleiben sichtbar; ein Verbindungsfehler erzeugt keine erfundenen Messwerte. Ausschalten stoppt Wetteranfragen und erhält den Cache. [Datenanbieter: Open-Meteo](https://open-meteo.com/).

### Zwei tatsächlich unterschiedliche Kontrollzentren

**iOS:** die ersten vier gewählten Befehle in einem Modul mit runden Schaltern, ein kompakter Player sowie vertikale Helligkeits- und Lautstärkeregler. **Android:** große Uhr, breite Material-Kacheln in zwei Spalten, horizontale Regler und ein Player über die gesamte Breite. Auf schmaler Fläche werden die Module angepasst. Ausgewählte Befehle, Reihenfolge und lange Tastendrücke bleiben erhalten. Player und Regler sind einzeln ein- und ausblendbar. Zustände stammen aus Android; geschützte Funktionen öffnen die passenden Systemeinstellungen. Schließen, Zurück und die konfigurierten Gesten bleiben verfügbar.

## Werkzeuge

| PRISME | Werkzeuge |
| --- | --- |
| Timer | Bis zu acht benannte Countdowns, Pause/Fortsetzen, Neustart und Löschen; Startdauer 1–1.440 Minuten. |
| Musik | Einfache Medientasten oder optional Titel/Künstler und Sitzungswahl. Der Player muss entsprechende Sitzungen bereitstellen. |
| Rechner und Umrechner | Dezimalrechnung mit vier Grundrechenarten; Länge, Masse, Temperatur und Dauer ohne Netzwerk. |
| Notizen und Aufgaben | Automatisch gespeichertes lokales Notizbuch und Checkliste; Zugriff über Suche und Karten. |
| Zeitwerkzeuge | Stoppuhr mit Runden, Konzentrationstimer und Tischuhr. |
| Kontrollzentrum | iOS-/Android-Ansichten, Reihenfolge, Farben, Gesten, Größen und globale Drehung. Einige Tasten öffnen Android-Einstellungen. |
| Lautstärke und Akku | Schwebende Lautstärke; 24 Akkuanzeigen, zwölf Paletten und Diagnose der Firmwarewerte. |
| Studio / Sense / Dock / Doctor | Klassische Widgets, lokale Zeit-/Laderegeln, optionales Dock und lokale Gerätediagnose. |

## Installation und Wechsel von V5

1. Vorhandene Konfiguration unter Einstellungen → Profile exportieren und aufbewahren.
2. Die signierte PRISME-6.0.1.apk herunterladen und über PRISME installieren. Eine normale Aktualisierung erfordert keine Deinstallation.
3. Das ursprüngliche Signaturzertifikat bleibt erhalten; Notizen, Aufgaben und Einstellungen bleiben bestehen.
4. PRISME als Start-App wählen, falls die Firmware dies erlaubt, oder als normale Anwendung öffnen.
5. Schwebende Funktionen separat autorisieren. Der modulare Startbildschirm benötigt keinen Benachrichtigungszugriff.

```text
adb install -r PRISME-6.0.1.apk
```

Alte JSON-Profile werden akzeptiert. V6 exportiert **Schema 7**: Das ist die Dateiformatnummer, keine App-Version 7. Für eine Rückkehr einen V5-Export behalten. Android kann eine niedrigere versionCode ablehnen; PRISME löscht zum Downgrade keine Daten automatisch.

## PC-Editor: erstellen und übertragen

1. Online-Editor öffnen oder das PC-ZIP vollständig entpacken und index.html starten. Unter Windows ist auch LANCER-EDITEUR.cmd verfügbar.
2. Einen vollständigen JSON-Export von PRISME laden. Ohne Export bleibt der Editor eine Vorschau und verhindert unvollständige Profildateien.
3. Zwischen Widgets der 22 Stile und optionalen Rasterseiten wählen. Widget-Reihenfolge und Darstellung oder Kartenposition, Größe, Palette und große Schrift einstellen. Kollisionen im Raster werden abgewiesen.
4. PRISME-6-Studio.json exportieren und per USB auf die Uhr kopieren.
5. Unter Einstellungen → Profile → Konfiguration importieren die Android-Dateiauswahl öffnen, Datei wählen und bestätigen.

Der Editor verarbeitet den Inhalt im Browserspeicher und lädt ihn nicht hoch. Andere Profilbereiche bleiben erhalten. Die Wettervorschau am PC verwendet gekennzeichnete Beispieldaten; die Stadt und die Netzwerkfreigabe werden auf der Uhr gewählt. Der Editor kann Einheiten und Aktualisierungsintervall übertragen. classic-studio.html bearbeitet weiterhin die 22 klassischen Desktops und ihre vier bisherigen Widgets. Die PC-Oberfläche und der ausführliche PC-Leitfaden sind derzeit französisch. Kein Server, Konto oder Entwicklungswerkzeug ist nötig.

## Berechtigungen und Wiederherstellung

Keine Werbung, kein Konto und keine Telemetrie. PRISME 6 deklariert **INTERNET** für die optionale Wetterfunktion; Wetteranfragen sind standardmäßig ausgeschaltet. Notizen, Aufgaben, Favoriten und andere Profildaten werden nicht an den Wetteranbieter übertragen. Die Freigabe, Stadt und Koordinaten bleiben lokal auf der Uhr und werden nicht durch ein Design importiert. Ein normaler JSON-Export ist unverschlüsselt und kann persönliche Notizen, Aufgaben oder Favoriten enthalten. Solche Dateien nicht öffentlich posten.

Erweiterte Musik ist standardmäßig aus. Android verlangt dafür die sensible Freigabe des Benachrichtigungszugriffs; PRISME nutzt Mediensitzungen, ohne Nachrichtentexte zu lesen oder zu speichern. Overlay-, Systemeinstellungs- und Nicht-stören-Zugriffe haben getrennte Zwecke.

Automatische Wiederherstellungspunkte liegen im privaten App-Speicher; zehn automatische Punkte bleiben erhalten. Im Werkzeugmenü sind exportierbare AES-256-GCM-Sicherungen mit mindestens zehn Passwortzeichen verfügbar. Ohne Passwort gibt es keine Fernwiederherstellung. Importdateien werden vor dem Schreiben validiert; ein Journal schützt unterbrochene Wiederherstellungen. Laufende Timer und Android-Freigaben werden nicht aus Themen importiert. Nach drei aufeinanderfolgenden unbehandelten Abstürzen bietet der Rettungsmodus einen Neustart oder den klassischen Desktop mit deaktivierten Overlays.

Overlays werden über dem modularen Startbildschirm verborgen und in anderen Apps gemäß den lokalen Einstellungen wieder angezeigt. Eine gemeinsame Pause/Fortsetzen-Funktion ist im Werkzeugmenü verfügbar.

## Prüfungen und Grenzen

Die bisherige V6-Validierung dokumentiert **67.407 Java-Prüfungen**, **1.775 Prüfungen des PC-Modells**, Galerietests, echte Edge-Browserabläufe und **16 Prüfungen auf der DM100** für Migration, Validierung, Wiederherstellung, Verschlüsselung, Alarme und eine synthetische Mediensitzung. Taktile Abläufe prüfen Seiten, Rasterbewegung, Paletten und den manuellen Rettungsmodus. Den aktuellen Stand der zusätzlichen Widget-, Wetter- und Kontrollzentrum-Prüfungen nennt der [Validierungsbericht](V6-VALIDATION.md); die bisherigen Zahlen belegen nicht automatisch diese neuen Abläufe.

Modellfälle sind keine entsprechend große Zahl physisch getesteter Uhren. Andere Geräte, Langzeitautonomie, alle Drittanbieter-Player und Play-Protect-Zertifizierung werden nicht zugesichert. Firmware und Android bestimmen Hintergrundwiedergabe, Alarmzustellung, Overlays und Rotation. Sieben historische Oberflächensprachen bleiben erhalten; einige ausführliche neue V6-Meldungen sind noch französisch.

## Dokumentation und Hilfe

Für Fehlerberichte Modell, Android-API, PRISME-Version, Ausrichtung und genaue Schritte nennen. Keine Konten, Seriennummern, privaten Exporte oder Rohprotokolle veröffentlichen. MIT-Lizenz; Markennamen der Stile bedeuten keine Zugehörigkeit oder Hardwarefreigabe.

[V6 · FR](V6-GUIDE.md) · [PC Studio · FR](PC-STUDIO.md) · [English guide](README.en.md) · [Compatibility](COMPATIBILITY.md) · [22 styles](STYLES.md) · [Privacy](PRIVACY.md) · [Validation](V6-VALIDATION.md) · [Build](BUILD.md) · [MIT](../LICENSE) · [Issues](https://github.com/s00r1/PRISME/issues/new/choose)
