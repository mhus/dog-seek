package com.example.dogseek.service;

import com.example.dogseek.model.Edge;
import com.example.dogseek.model.GameMap;
import com.example.dogseek.model.Item;
import com.example.dogseek.model.ItemTemplate;
import com.example.dogseek.model.Node;
import com.example.dogseek.model.PlayerState;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Spiellogik. UI wird an GameUI delegiert.
 */
@Service
public class GameEngine implements CommandLineRunner {

    private final TerrainProvider terrainProvider;
    private final GameStateService gameStateService;
    private final PlacesService placesService;
    private final GameUI ui;

    private final List<String> eventLog = new ArrayList<>();
    private final Random random = new Random();

    public GameEngine(TerrainProvider terrainProvider,
                      GameStateService gameStateService,
                      PlacesService placesService,
                      GameUI ui) {
        this.terrainProvider = terrainProvider;
        this.gameStateService = gameStateService;
        this.placesService = placesService;
        this.ui = ui;
    }

    // ---------- Hilfsmethoden ----------
    private void addEvent(String message) {
        eventLog.add(message);
    }

    private String dogName() {
        return gameStateService.getDogName();
    }

    private PlayerState player() {
        return gameStateService.getPlayerState();
    }

    private boolean hasItem(String itemId) {
        return player().getInventory().stream().anyMatch(i -> i.getId().equals(itemId));
    }

    private List<Edge> edgesFrom(Node node, GameMap map) {
        return map.getEdges().stream()
                .filter(e -> e.getSource().getId().equals(node.getId()))
                .collect(Collectors.toList());
    }

    /** Hund in einen anderen erlaubten Raum umverteilen. */
    private void relocateDog(GameMap map, Node fromNode, Item dog) {
        fromNode.getItems().remove(dog);
        dog.setDiscovered(false); // im neuen Raum wieder versteckt
        List<Node> validNodes = map.getNodes();
        Optional<ItemTemplate> tpl = placesService.getAllItems().stream()
                .filter(i -> i.getId().equals("hund")).findFirst();
        if (tpl.isPresent() && tpl.get().getAllowedRooms() != null && !tpl.get().getAllowedRooms().isEmpty()) {
            List<String> allowed = tpl.get().getAllowedRooms();
            validNodes = map.getNodes().stream()
                    .filter(n -> allowed.contains(n.getId())).collect(Collectors.toList());
        }
        if (validNodes.isEmpty()) validNodes = map.getNodes();
        validNodes.get(random.nextInt(validNodes.size())).getItems().add(dog);
    }

    /** true, wenn der Raum aktuell sichtbar ist (hell oder Taschenlampe). */
    private boolean isVisible(Node node) {
        return !node.isDark() || hasItem("taschenlampe");
    }

    /** Items im Raum, die bereits per 'Suchen' gefunden wurden. */
    private List<Item> discoveredItems(Node node) {
        return node.getItems().stream()
                .filter(Item::isDiscovered)
                .collect(Collectors.toList());
    }

    // ---------- Aktionen ----------
    private boolean tryMove(Edge edge, GameMap map) {
        if (edge.isLocked()) {
            edge.setAttempted(true);
            if (!hasItem(edge.getRequiredKeyId())) {
                addEvent("Die Tür (" + edge.getName() + ") ist verschlossen! Das Schloss lässt sich nicht öffnen.");
                return false;
            }
            addEvent("Einer deiner Schlüssel passt! Du schließt die Tür auf.");
            edge.setLocked(false);
        }

        player().setCurrentNode(edge.getTarget());
        addEvent("Du gehst durch: " + edge.getName());

        // Beim Verlassen den Suchfortschritt des alten Raums zurücksetzen
        edge.getSource().setSearchCount(0);

        // --- Beiß-Logik beim Betreten ---
        Node newNode = edge.getTarget();
        Optional<Item> belloOpt = newNode.getItems().stream()
                .filter(i -> i.getId().equals("hund")).findFirst();
        if (belloOpt.isPresent()) {
            // Hund wird beim Betreten automatisch entdeckt
            belloOpt.get().setDiscovered(true);
            addEvent("ACHTUNG: " + dogName() + " ist hier!");
            if (hasItem("wurst")) {
                addEvent(dogName() + " fletscht die Zähne, aber er riecht die Wurst und bleibt ruhig sitzen.");
            } else {
                addEvent(dogName() + " erkennt dich im Dunkeln nicht, gerät in Panik und BEISST DICH!");
                player().setLives(player().getLives() - 1);
                if (player().getLives() <= 0) {
                    addEvent("Du wurdest zu oft gebissen und musst ins Krankenhaus.");
                } else {
                    addEvent("Du hast ein Leben verloren!");
                    addEvent(dogName() + " flieht panisch in einen anderen Raum!");
                    relocateDog(map, newNode, belloOpt.get());
                }
            }
        }
        return true;
    }

    private void doSearch(Node currentNode) {
        if (!isVisible(currentNode)) {
            addEvent("Es ist viel zu dunkel, um hier etwas zu finden. Du ertastest nur Staub.");
            return;
        }
        addEvent("Du suchst gründlich...");
        List<Item> itemsHere = currentNode.getItems();
        if (itemsHere.isEmpty()) {
            addEvent("Hier ist nichts Interessantes zu finden.");
            return;
        }

        List<Item> undiscovered = itemsHere.stream()
                .filter(i -> !i.isDiscovered())
                .collect(Collectors.toList());
        if (undiscovered.isEmpty()) {
            addEvent("Du hast bereits alles durchsucht. Hier ist nichts mehr zu finden.");
            return;
        }

        if (currentNode.getSearchCount() == 0) {
            // Erster Suchvorgang: selten (ca. 25%) wird ein zufälliges Item
            // übersehen und erst beim zweiten Suchen gefunden.
            List<Item> heldBack = new ArrayList<>();
            if (undiscovered.size() >= 2 && random.nextDouble() < 0.25) {
                Item hidden = undiscovered.get(random.nextInt(undiscovered.size()));
                heldBack.add(hidden);
            }
            List<Item> newlyFound = new ArrayList<>();
            for (Item it : undiscovered) {
                if (!heldBack.contains(it)) {
                    it.setDiscovered(true);
                    newlyFound.add(it);
                }
            }
            if (!newlyFound.isEmpty()) {
                addEvent("Du findest: " + newlyFound.stream().map(Item::getName).collect(Collectors.joining(", ")));
            } else {
                addEvent("Diesmal findest du nichts. Vielleicht nochmal suchen?");
            }
            if (!heldBack.isEmpty()) {
                addEvent("Du hast das Gefühl, als hättest du etwas übersehen...");
            }
        } else {
            // Zweites (und weiteres) Suchen: jetzt wirklich alles finden.
            for (Item it : undiscovered) {
                it.setDiscovered(true);
            }
            addEvent("Du findest noch: " + undiscovered.stream().map(Item::getName).collect(Collectors.joining(", ")));
        }
        currentNode.setSearchCount(currentNode.getSearchCount() + 1);
    }

    /** Untermenü: Items im Raum und Inventar ansehen. */
    private void doExamine(Node currentNode) {
        List<Item> roomItems = discoveredItems(currentNode);
        List<Item> invItems = new ArrayList<>(player().getInventory());

        if (roomItems.isEmpty() && invItems.isEmpty()) {
            addEvent("Hier gibt es nichts, was du dir ansehen könntest.");
            return;
        }

        while (true) {
            int[] sel = ui.drawExamineMenu(currentNode, roomItems, invItems);
            if (sel[0] == 0) return;
            if (sel[0] == -1) continue;

            Item item;
            if (sel[0] == 1) {
                item = roomItems.get(sel[1]);
            } else {
                item = invItems.get(sel[1]);
            }
            ui.drawItemDescription(item);
        }
    }

    /** Untermenü: Items im Raum aufheben. Liefert true bei Sieg. */
    private boolean doPickup(Node currentNode, GameMap map) {
        if (!isVisible(currentNode)) {
            addEvent("Es ist viel zu dunkel, um hier etwas zu finden. Du ertastest nur Staub.");
            return false;
        }
        List<Item> itemsHere = discoveredItems(currentNode);
        if (itemsHere.isEmpty()) {
            addEvent("Hier liegt nichts, was du aufheben könntest. Vielleicht musst du erst suchen.");
            return false;
        }

        while (true) {
            int choice = ui.drawPickupMenu(currentNode, itemsHere);
            if (choice == 0) return false;
            if (choice == -2) {
                // "Alles aufheben": hebt wirklich jedes Item auf.
                // Sichere Items landen im Inventar, gefährliche kosten ein Leben,
                // der Hund löst die Fang-Logik aus (Sieg mit Powerriegel, Flucht ohne).
                addEvent("Du hebst alles auf, was du finden kannst...");
                boolean won = false;
                for (Item it : new ArrayList<>(itemsHere)) {
                    if (it.getId().equals("hund")) {
                        addEvent("Du versuchst " + dogName() + " zu greifen...");
                        if (hasItem("powerriegel")) {
                            addEvent("Du hältst " + dogName() + " den Powerriegel hin. Er liebt das Zeug!");
                            addEvent("HERZLICHEN GLÜCKWUNSCH! Du konntest " + dogName() + " sicher einfangen. Das Spiel ist gewonnen!");
                            currentNode.getItems().remove(it);
                            itemsHere.remove(it);
                            won = true;
                            break;
                        } else {
                            addEvent(dogName() + " ist zu misstrauisch. Ohne den Powerriegel lässt er sich nicht anlocken!");
                            addEvent("Er entwitscht dir und rennt weg!");
                            currentNode.getItems().remove(it);
                            itemsHere.remove(it);
                            relocateDog(map, currentNode, it);
                        }
                    } else if (it.isDangerous()) {
                        addEvent("Du greifst nach '" + it.getName() + "' und stichst dich! AUA!");
                        currentNode.getItems().remove(it);
                        itemsHere.remove(it);
                        player().setLives(player().getLives() - 1);
                        if (player().getLives() <= 0) {
                            addEvent("Du hast dich so unglücklich verletzt, dass du das Bewusstsein verlierst.");
                            addEvent("*** GAME OVER ***");
                            return false;
                        }
                        addEvent("Du hast ein Leben verloren! (Noch " + player().getLives() + " Leben)");
                    } else {
                        player().getInventory().add(it);
                        currentNode.getItems().remove(it);
                        itemsHere.remove(it);
                        addEvent("  - '" + it.getName() + "' aufgehoben.");
                    }
                }
                if (player().getLives() <= 0) {
                    addEvent("*** GAME OVER ***");
                    return false;
                }
                return won;
            }
            if (choice < 1 || choice > itemsHere.size()) continue;

            Item item = itemsHere.get(choice - 1);

            if (item.getId().equals("hund")) {
                addEvent("Du versuchst " + dogName() + " zu greifen...");
                if (hasItem("powerriegel")) {
                    addEvent("Du hältst " + dogName() + " den Powerriegel hin. Er liebt das Zeug!");
                    addEvent("HERZLICHEN GLÜCKWUNSCH! Du konntest " + dogName() + " sicher einfangen. Das Spiel ist gewonnen!");
                    currentNode.getItems().remove(item);
                    return true; // Sieg
                } else {
                    addEvent(dogName() + " ist zu misstrauisch. Ohne den Powerriegel lässt er sich nicht anlocken!");
                    addEvent("Er entwitscht dir und rennt weg!");
                    currentNode.getItems().remove(item);
                    relocateDog(map, currentNode, item);
                    return false;
                }
            } else if (item.isDangerous()) {
                // Gefährlicher Gegenstand: man sticht/schneidet sich, verliert ein Leben
                addEvent("Du greifst nach '" + item.getName() + "' und stichst dich! AUA!");
                currentNode.getItems().remove(item);
                itemsHere.remove(choice - 1);
                player().setLives(player().getLives() - 1);
                if (player().getLives() <= 0) {
                    addEvent("Du hast dich so unglücklich verletzt, dass du das Bewusstsein verlierst.");
                    return false;
                } else {
                    addEvent("Du hast ein Leben verloren! (Noch " + player().getLives() + " Leben)");
                    if (itemsHere.isEmpty()) return false;
                }
            } else {
                player().getInventory().add(item);
                currentNode.getItems().remove(item);
                itemsHere.remove(choice - 1);
                addEvent("Du hast '" + item.getName() + "' aufgehoben.");
                if (itemsHere.isEmpty()) return false;
            }
        }
    }

    /** Untermenü: Item aus dem Inventar benutzen. */
    private static final int MAX_LIVES = 3;

    private void doUse() {
        List<Item> inv = player().getInventory();
        if (inv.isEmpty()) {
            addEvent("Du hast nichts dabei, das du benutzen könntest.");
            return;
        }

        while (true) {
            int choice = ui.drawUseMenu(inv);
            if (choice == 0) return;
            if (choice < 1 || choice > inv.size()) continue;

            Item item = inv.get(choice - 1);

            if (item.getId().equals("wasser") || item.getId().equals("pflaster")) {
                if (player().getLives() >= MAX_LIVES) {
                    addEvent("Du hast schon volle Lebensenergie (" + MAX_LIVES + " Leben). Du sparst dir das " + item.getName() + " für später.");
                    continue; // Item nicht verbrauchen
                }
                player().setLives(player().getLives() + 1);
                if (item.getId().equals("wasser")) {
                    addEvent("Du trinkst das Wasser. Du fühlst dich erfrischt! (+1 Leben, jetzt " + player().getLives() + ")");
                } else {
                    addEvent("Du klebst dir das Pflaster auf einen Kratzer. (+1 Leben, jetzt " + player().getLives() + ")");
                }
                inv.remove(item);
                if (inv.isEmpty()) return;
            } else {
                addEvent("Das kannst du so nicht benutzen.");
                return;
            }
        }
    }

    // ---------- Haupt-Loop ----------
    @Override
    public void run(String... args) throws Exception {
        GameMap map = terrainProvider.getMap();
        gameStateService.init(new PlayerState(map.getStartNode()));

        // Zufälligen Hundenamen wählen
        List<String> dogNames = placesService.getDogNames();
        String dogName = "Bello";
        if (!dogNames.isEmpty()) {
            dogName = dogNames.get(random.nextInt(dogNames.size()));
        }
        gameStateService.setDogName(dogName);

        // Name des Hund-Items in der Map aktualisieren
        for (Node n : map.getNodes()) {
            for (Item it : n.getItems()) {
                if (it.getId().equals("hund")) {
                    it.setName(dogName + " der Hund");
                }
            }
        }

        addEvent("Dein treuer Hund '" + dogName + "' ist im großen Haus weggelaufen.");
        addEvent("Es wird langsam dunkel und du musst ihn finden.");
        addEvent("Wähle eine Zahl zum Gehen oder einen Buchstaben (s/p/l/u/q) für Aktionen.");

        ui.init();
        boolean running = true;
        boolean won = false;

        while (running) {
            Node currentNode = player().getCurrentNode();
            List<Edge> edges = edgesFrom(currentNode, map);

            ui.drawMainScreen(currentNode, edges, player(), eventLog);
            eventLog.clear();

            if (player().getLives() <= 0) {
                ui.printLine("\n GAME OVER!");
                break;
            }

            String input = ui.readAction("\n Auswahl > ");

            int choice = -1;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                // keine Zahl -> als Buchstaben-Aktion prüfen
            }

            if (choice >= 1 && choice <= edges.size()) {
                tryMove(edges.get(choice - 1), map);
            } else if (input.equals("s")) {
                doSearch(currentNode);
            } else if (input.equals("p")) {
                if (doPickup(currentNode, map)) {
                    won = true;
                    running = false;
                }
            } else if (input.equals("l")) {
                doExamine(currentNode);
            } else if (input.equals("u")) {
                doUse();
            } else if (input.equals("q")) {
                addEvent("Du gibst die Suche für heute auf und gehst traurig nach Hause...");
                running = false;
            } else {
                addEvent("Ungültige Auswahl.");
            }
        }

        ui.drawMainScreen(player().getCurrentNode(), edgesFrom(player().getCurrentNode(), map), player(), eventLog);
        eventLog.clear();

        if (won) {
            ui.printLine("\n *** SIEG! Du hast " + dogName + " eingefangen! ***");
        }
        ui.printLine("\nDanke fürs Spielen!");
        System.exit(0);
    }
}
