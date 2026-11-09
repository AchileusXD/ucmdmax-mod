# UCMD Max

Fabric-Mod für Minecraft 26.2 (nur Client). Damit können Mitglieder der UCMD-Fraktion ihre Aktis
direkt im Spiel in unser Akti-Sheet eintragen, statt dafür aus dem Spiel raus zu müssen.

## Was die Mod macht

- Sie fügt den Befehl `/ucmdmax` hinzu. Der läuft nur im eigenen Client und wird nicht an den
  Minecraft-Server geschickt.
- `/ucmdmax newakti` öffnet ein Fenster: Kategorie wählen, Felder ausfüllen, eintragen.
- Die Eingaben gehen per HTTP an unseren Fraktions-Server (`http://130.61.21.127:8095`), und der
  trägt sie ins Akti-Sheet ein.

## Was die Mod nicht macht

- Sie greift nicht ins Gameplay ein, automatisiert nichts und verwendet keine Mixins.
- Aus dem Spiel nimmt sie nur den eigenen Minecraft-Namen und die UUID, und das nur beim
  einmaligen Verbinden.
- Außer zu dem Server oben baut sie keine Verbindungen auf.

## Befehle

| Befehl | Wofür |
|---|---|
| `/ucmdmax newakti` oder Taste `K` | Akti eintragen (Taste änderbar unter Optionen > Steuerung) |
| `/ucmdmax verbinden <Code>` | einmal verbinden (den Code gibt es in Discord mit `/modverbinden`) |
| `/ucmdmax status` | zeigt, ob der Server erreichbar ist und ob die Verbindung steht |
| `/ucmdmax trennen` | Verbindung lösen |
| `/ucmdmax server [Adresse]` | Server-Adresse anzeigen oder ändern |

## Installation

1. Fabric Loader für Minecraft 26.2 und Fabric API installieren.
2. Die Jar-Datei (`ucmdmax-<Version>.jar`) in den `mods`-Ordner legen.

## Selbst bauen

```
gradlew build
```

Die Mod liegt dann in `build/libs/` (z. B. `ucmdmax-1.3.0.jar`). Gradle braucht Java 25 und lädt es bei Bedarf
selbst herunter.

## Aufbau

```
src/main/java/at/ucmd/ucmdmax/
  UcmdMax.java        Start der Mod
  UcmdBefehle.java    /ucmdmax ...
  UcmdKonfig.java     config/ucmdmax.json (Server-Adresse, Schlüssel)
  api/                HTTP-Anfragen an unseren Server
  akti/               Kategorien und Felder, wie der Server sie schickt
  gui/                die Fenster (Laden, Kategorien, Formular, Auswahl, Ergebnis)
```
