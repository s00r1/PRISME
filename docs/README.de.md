# PRISME 5.1

[Français](README.fr.md) · [English](README.en.md) · [Deutsch](README.de.md) · [中文](README.zh.md) · [Türkçe](README.tr.md) · [Русский](README.ru.md) · [العربية](README.ar.md)

**Ein anpassbarer Startbildschirm für Uhren mit vollständigem Android.** Offline, ohne Konto, Werbung oder Telemetrie.

## Installation

**[Installierbare PRISME.apk herunterladen](https://github.com/s00r1/PRISME/releases/latest/download/PRISME.apk)** · [Versionen und Prüfsummen](https://github.com/s00r1/PRISME/releases)

Erforderlich sind **vollständiges Android ab 7.1, mindestens API 25**, und die Installation gewöhnlicher Android-APKs. APK im Browser der Uhr herunterladen, mit dem Android-Installer öffnen und gegebenenfalls die Installationsquelle erlauben. PRISME starten und als Start-App wählen, sofern die Firmware dies erlaubt, oder als normale App öffnen. Für schwebende Lautstärke und globale Ausrichtung „Über anderen Apps einblenden“ erlauben. Sprache unter **Einstellungen → Stil → Sprache** wählen. Offizielle Updates über die vorhandene Version installieren.

Vom Computer: `adb install -r PRISME.apk`. Root, Firmwarewechsel oder Änderungen an Google-Diensten sind zur Installation nicht nötig.

## Funktionen

- **22 unterschiedliche Anordnungen**: Kalender, Desktop, Galerie, Terminal, Arcade, Konstellation, Textverzeichnis, Mosaik, Waben, Instrumente, Notizbuch und Minimalansicht.
- Ständige Navigation zu nativem Zifferblatt, Stilen, Heute und Einstellungen. Heute gedrückt halten öffnet fünf anpassbare Verknüpfungen.
- **Kontrollzentrum im iOS- oder Android-Stil**: Schaltflächen, Reihenfolge, Farben, Transparenz, Größe, Position und Gesten. Manche Befehle öffnen die Android-Einstellungen.
- **Hochformat / Querformat** per kurzem Tippen; langes Drücken bietet umgekehrte Ausrichtungen und Systemmodus. Drehbefehle auch in der Benachrichtigung. Auf DM100 geprüft; andere Firmware kann die Funktion einschränken.
- **Schwebender Lautstärkeregler** mit Stilen, Formen, Größe, Deckkraft, Position, ausklappbaren Tasten und optionalem Verblassen.
- App-/Einstellungssuche, Favoriten, zuletzt verwendete Apps, Arbeitsbereiche und eigene Namen.
- Heute, automatisch gespeicherte Notizen, Aufgaben, Android-Alarmtimer, Stoppuhr mit Runden und Tischuhr.
- Gesamtskalierung, Ränder, Schrift-/Symbolgrößen, Farben, Profile und lokale Sicherungen.
- **Französisch, Englisch, Deutsch, vereinfachtes Chinesisch, Türkisch, Russisch und Arabisch**, einschließlich Rechts-nach-links-Darstellung.

## Uhren

| Modell oder Familie | Status |
|---|---|
| Domiwear / Domino DM100, Android 7.1.1 | **Auf Gerät getestet**, Referenz 640 × 480 im Querformat |
| Andere DM100-Varianten | Einzeln prüfen; gleicher Name bedeutet nicht gleiche Firmware |
| LEMFO LEM T / LEM16 | **Ungetestete** Kandidaten für Community-Tests bei erfüllten Android-Anforderungen |
| KOSPET PRIME / PRIME SE / PRIME Pro / Optimus 2 | **Ungetestet**; keine Garantie für runde Displays oder globale Drehung |
| Andere Uhren mit vollständigem Android, API 25+ | Installation und Funktionen müssen geprüft werden |

Ungetestete Modelle sind **Kandidaten, keine zertifizierte Liste**. Betriebssystem, API und APK-Installation auf der echten Uhr prüfen. Dies ist kein Wear-OS-Zifferblatt. Pixel Watch und Wear-OS-Galaxy-Watch sind nicht validiert. watchOS, Tizen und proprietäre Systeme ohne Android-APK-Installation liegen außerhalb des Ziels. [Kriterien und Quellen](COMPATIBILITY.md).

## Bilder, Datenschutz und Grenzen

[Menüs und Optionen](SCREENSHOTS.md) · [Alle 22 Stile](STYLES.md)

Die Bilder zeigen echte Oberflächen eines getrennten Demo-Builds mit erfundenen Daten und allgemeinen Symbolen, keine Benutzerkonten oder persönliche App-Liste. Keine Internet-Berechtigung. Notizen, Aufgaben, Favoriten und Starts aus PRISME bleiben lokal; eigene Exporte können diese Daten enthalten. Keine privaten Schlüssel, Benutzer-Backups, Gerätekennungen oder persönlichen Protokolle werden veröffentlicht.

Hersteller-Energiesparen kann Timer und schwebende Dienste beenden. Musiksteuerung benötigt einen aktiven Player. Übersetzungen können verfeinert werden. Für die gemeldeten seitlichen schwarzen Bereiche auf einer DM100 gibt es **keine bestätigte Reparatur**. PRISME verändert weder Displaypanel noch Treiber. Die Darstellung auf anderen Bildschirmen ist nicht garantiert.

Java-Code, Ressourcen, Sprachdateien, Tests und optionaler Imou-Begleiter sind enthalten. [Erstellen und testen](BUILD.md) · [Berechtigungen](PRIVACY.md) · [Kompatibilität melden](https://github.com/s00r1/PRISME/issues/new/choose). Der Code ist einsehbar; eine Open-Source-Lizenz wurde noch nicht gewählt. Die APK darf kostenlos heruntergeladen, installiert und verwendet werden. Markennamen beschreiben gestalterische Inspiration, ohne Verbindung zu den Marken.

![PRISME — de](../assets/screenshots/language-de.png)
