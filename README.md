# 🐕 Dog-Hunt

Ein textbasiertes Konsolen-Spiel in Java mit Spring Boot: Dein Hund ist im großen,
dunklen Haus weggelaufen. Deine Aufgabe ist es, ihn zu finden und einzufangen –
bevor er dich findet und beißt!

## ✨ Features

- **Prozedural generiertes Haus:** Feste Räume (Eingang, Flur EG, Treppe, Flur OG, Bad)
  plus zufällig zugewiesene Zimmer (Küche, Wohnzimmer, Schlafzimmer, Kinderzimmer).
- **Zufälliger Hundename:** Bei jedem Spielstart wird einer von 12 voreingestellten
  Namen gewählt.
- **Dunkle Räume:** ~60 % der Räume sind stockfinster. Ohne Taschenlampe kannst du
  dort weder suchen noch etwas aufheben.
- **Verschlossene Türen:** Bestimmte Türen (Eingang, Badezimmer) sind verschlossen
  und brauchen den passenden Schlüssel. Erst nach dem ersten Öffnungsversuch zeigt
  die Tür `[Verschlossen]` an.
- **Items mit Beschreibungen:** Jeder Gegenstand hat eine kurze Erklärung, die du
  dir vor dem Aufheben ansehen kannst.
- **Gefährliche Gegenstände:** Der rostige Nagel sticht – wer ihn unvorsichtig
  aufhebt, verliert ein Leben.
- **Lebenssystem:** Maximal 3 Leben, dargestellt als Sternchen (`* *`).
  Heil-Items (Wasser, Pflaster) wirken nur, wenn du nicht schon bei voller
  Lebensenergie bist – sonst werden sie nicht verbraucht.
- **Sperriges Suchen:** Beim ersten Suchen werden selten (ca. 25 %) Items
  übersehen, die erst beim zweiten Suchen gefunden werden. Beim Verlassen des
  Raums wird der Suchfortschritt zurückgesetzt.
- **Hund-Logik:** Betrittst du den Raum mit dem Hund ohne Wurst, beißt er dich
  und flieht in einen anderen Raum. Mit Powerriegel + Wurst kannst du ihn
  einfangen – mit nur Powerriegel flieht er misstrauisch.
- **ANSI-TUI:** Farbige Text-Oberfläche mit klarer Struktur direkt im Terminal.

## 🎮 Steuerung

### Hauptmenü
| Taste | Aktion |
|:-----:|--------|
| `1`, `2`, … | Weg gehen (nummeriert) |
| `s` | **S**uchen |
| `p` | **P**ick up (Aufheben) |
| `l` | **L**ook (Ansehen) |
| `u` | **U**se (Benutzen) |
| `q` | **Q**uit (Beenden) |

### Untermenüs (Aufheben, Ansehen, Benutzen)
- Items werden **nummeriert** ausgewählt (`1`, `2`, …), damit keine Buchstaben
  mit Item-Namen kollidieren.
- `z` führt **Zurück** ins Hauptmenü.

### Item-Beschreibung
- `z` oder `Enter` kehrt zurück.

## 📋 Voraussetzungen

- **Java 17** oder neuer
- **Maven 3.6+**

## 🚀 Build & Start

```bash
# Bauen (Tests überspringen)
mvn clean package -DskipTests

# Spiel starten
java -jar target/dog-hunt-1.0-SNAPSHOT.jar
```

Oder direkt mit Spring Boot:

```bash
mvn spring-boot:run
```

## 🏗️ Projektstruktur

```
dog-hunt/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/example/doghunt/
    │   ├── DogHuntApplication.java      # Spring-Boot-Einstiegspunkt
    │   ├── model/                       # Datenmodelle
    │   │   ├── Node.java                # Raum (Knoten im Graphen)
    │   │   ├── Edge.java                # Verbindung zwischen Räumen
    │   │   ├── GameMap.java             # Gesamtkarte
    │   │   ├── Item.java                # Konkretes Item im Spiel
    │   │   ├── ItemTemplate.java        # Item-Vorlage (aus JSON)
    │   │   ├── PlaceTemplate.java       # Raum-Vorlage (aus JSON)
    │   │   ├── ConnectionTemplate.java  # Verbindungs-Vorlage (aus JSON)
    │   │   ├── GameConfig.java          # Wurzel der JSON-Konfiguration
    │   │   └── PlayerState.java         # Spielerzustand
    │   └── service/
    │       ├── PlacesService.java       # Lädt places.json (Jackson)
    │       ├── TerrainGenerator.java    # Erzeugt die Karte (zufällig)
    │       ├── TerrainProvider.java     # Hält die generierte Karte
    │       ├── GameStateService.java    # Hält Spielerzustand + Hundenamen
    │       ├── GameUI.java              # Reines Rendering + Input
    │       └── GameEngine.java          # Spiellogik + Haupt-Loop
    └── resources/
        └── places.json                  # Räume, Items, Hundenamen
```

### Architektur-Prinzipien

- **Trennung von Logik und Darstellung:** `GameUI` kennt ausschließlich Rendering
  und Eingabe. `GameEngine` enthält die gesamte Spiellogik und ruft die UI nur
  zur Anzeige auf.
- **Konfiguration über JSON:** Alle Räume, Verbindungen, Items und Hundenamen
  werden aus `src/main/resources/places.json` geladen. Keine Änderung am Code
  nötig, um das Spiel zu balancen.
- **Spring Boot als Container:** Die Services sind `@Service` / `@Component`,
  werden von Spring instanziiert und injiziert. Der `GameEngine` ist ein
  `CommandLineRunner` und startet das Spiel direkt nach dem Boot.
- **Prozedurale Generierung:** Der `TerrainGenerator` baut die Karte in mehreren
  Schritten auf:
  1. Feste Räume aus den Templates laden.
  2. Slot-Räume (Küche, Wohnzimmer, etc.) zufällig den Slots in Flur EG / OG
     zuweisen.
  3. Feste Verbindungen plus zufällige Slot-Verbindungen erzeugen
     (Rückwege werden nur auto-generiert, wenn keine explizite Rückverbindung
     existiert – das verhindert doppelte/umgehende Kanten, die z. B. das
     Eingangsschloss aushebeln würden).
  4. ~60 % der Räume zufällig als „dunkel" markieren (Eingang bleibt hell).
  5. Items entsprechend ihrer `allowedRooms`-Regel auf erlaubte Räume verteilen.

## ⚙️ Konfiguration (`places.json`)

Die Datei `src/main/resources/places.json` definiert:

- **`places[]`** – Raumvorlagen mit ID, Name, Beschreibung und Verbindungen.
  Verbindungen können `locked: true` und `requiredKeyId` haben.
- **`items[]`** – Itemvorlagen mit ID, Name, Beschreibung, `totalCount`,
  optional `allowedRooms` (sonst überall erlaubt) und optional `dangerous: true`.
- **`dogNames[]`** – Liste möglicher Hundenamen.

Beispiel-Item mit Gefahr:

```json
{
  "id": "nagel",
  "name": "Ein rostiger Nagel",
  "description": "Ein alter, rostiger Nagel. Er ist spitz und sieht gefährlich aus!",
  "totalCount": 3,
  "dangerous": true
}
```

## 🎯 Spielziel

Fange den Hund ein, indem du:

1. Den passenden **Powerriegel** findest (erlaubte Räume: alle außer Eingang/Bad).
2. Den **Hund** aufspürst (Wohnzimmer, Schlafzimmer oder Kinderzimmer).
3. Den Hund aufhebst (`p` → Auswahl). Mit Powerriegel im Inventar klappt das!
4. **Optional:** Nimm eine **Wurst** mit, um den Hund beim ersten Betreten seines
   Raums zu beruhigen (kein Biss).

Verlierst du alle 3 Leben → Game Over.

## 🛠️ Technologie-Stack

- **Spring Boot 3.2.4** – DI-Container, `CommandLineRunner`
- **Jackson** (`spring-boot-starter-json`) – JSON-Konfiguration
- **Java 17** – Sprache
- **ANSI-Escape-Codes** – für die Terminal-UI (keine externe TUI-Bibliothek)

## 📜 Lizenz

Privates Lern-/Spielprojekt – frei verwendbar.
