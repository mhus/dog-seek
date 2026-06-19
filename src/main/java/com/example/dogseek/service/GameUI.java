package com.example.dogseek.service;

import com.example.dogseek.model.Edge;
import com.example.dogseek.model.Item;
import com.example.dogseek.model.Node;
import com.example.dogseek.model.PlayerState;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Reines UI: Zeichnet das TUI und liest Benutzereingaben.
 * Kennt keine Spiellogik.
 */
@Component
public class GameUI {

    private Scanner scanner;

    public void init() {
        this.scanner = new Scanner(System.in);
    }

    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /** Liest eine Zahl ein. Liefert -1 bei ungültiger Eingabe. */
    public int readChoice(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.equalsIgnoreCase("z")) return 0; // 'z' = Zurück
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Liest eine Eingabe und liefert sie klein-geschrieben zurück (für Buchstaben-Aktionen). */
    public String readAction(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim().toLowerCase();
    }

    public void printLine(String msg) {
        System.out.println(msg);
    }

    /** Zeichnet den Hauptbildschirm. */
    public void drawMainScreen(Node currentNode, List<Edge> edges, PlayerState player, List<String> eventLog) {
        clearScreen();

        System.out.println("================================================================================");
        System.out.println("                                  DOG-HUNT                                      ");
        System.out.println("================================================================================");
        System.out.printf(" ORT: %-46s LEBEN: %s%n", currentNode.getName(), livesAsStars(player.getLives()));
        System.out.println("--------------------------------------------------------------------------------");

        boolean hasFlashlight = player.getInventory().stream()
                .anyMatch(i -> i.getId().equals("taschenlampe"));
        String desc = currentNode.getDescription();
        if (currentNode.isDark() && !hasFlashlight) {
            desc = "Es ist hier stockfinster! Du kannst kaum etwas erkennen.";
        } else if (currentNode.isDark()) {
            desc += " (Du leuchtest den dunklen Raum aus.)";
        }
        System.out.println(" " + desc);
        System.out.println();

        List<Item> inv = player.getInventory();
        String invStr = inv.isEmpty() ? "(leer)" : formatInventory(inv);
        System.out.println(" INVENTAR: " + invStr);
        System.out.println("--------------------------------------------------------------------------------");

        System.out.println(" WEGE (Zahl eingeben zum Gehen):");
        if (edges.isEmpty()) {
            System.out.println(" (Keine Wege von hier aus)");
        }
        int idx = 1;
        for (Edge e : edges) {
            String line = String.format(" [%d] %s -> %s", idx, e.getName(), e.getTarget().getName());
            if (e.isLocked() && e.isAttempted()) line += "  [Verschlossen]";
            System.out.println(line);
            idx++;
        }
        System.out.println();
        System.out.println(" AKTIONEN:");
        System.out.println(" [s] Suchen");
        System.out.println(" [p] Aufheben");
        System.out.println(" [l] Ansehen");
        System.out.println(" [u] Benutzen");
        System.out.println(" [q] Beenden");

        System.out.println("================================================================================");
        System.out.println(" EREIGNISSE:");
        if (eventLog.isEmpty()) {
            System.out.println(" (Nichts Neues passiert.)");
        } else {
            for (String event : eventLog) {
                System.out.println(" " + event);
            }
        }
        System.out.println("================================================================================");
    }

    /** Untermenü Aufheben. Liefert 0=Zurück, 1..n=Auswahl. */
    /**
     * Untermenü Aufheben.
     * Rückgabe: 0 = Zurück, -2 = Alles aufheben, 1..n = Auswahl, -1 = ungültig.
     */
    public int drawPickupMenu(Node currentNode, List<Item> itemsHere) {
        clearScreen();
        System.out.println("================================================================================");
        System.out.println("  AUFHEBEN in: " + currentNode.getName());
        System.out.println("================================================================================");
        System.out.println(" Was möchtest du aufheben?");
        System.out.println();
        System.out.println(" [a] Alles aufheben (Vorsicht: auch gefährliche Gegenstände!)");
        for (int i = 0; i < itemsHere.size(); i++) {
            System.out.println(" [" + (i + 1) + "] " + itemsHere.get(i).getName());
        }
        System.out.println(" [z] Zurück");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.print(" Auswahl > ");
        String input = scanner.nextLine().trim();
        if (input.equalsIgnoreCase("z")) return 0;
        if (input.equalsIgnoreCase("a")) return -2;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Wandelt eine Leben-Anzahl in Sternchen um: 2 -> "* *", 1 -> "*", 0 -> "-". */
    private String livesAsStars(int lives) {
        if (lives <= 0) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lives; i++) {
            if (i > 0) sb.append(" ");
            sb.append("*");
        }
        return sb.toString();
    }

    /** Formatiert das Inventar als kommaseparierte Liste, gleiche Items werden
     *  kumuliert: einzelne Items ohne Zahl, mehrere als "N x Name".
     *  Beispiel: "Schlüssel, 3 x Powerriegel, Taschenlampe". */
    private String formatInventory(List<Item> inv) {
        // Reihenfolge des ersten Vorkommens beibehalten
        LinkedHashMap<String, Item> firstById = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Item i : inv) {
            firstById.putIfAbsent(i.getId(), i);
            counts.merge(i.getId(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .map(e -> {
                    String name = firstById.get(e.getKey()).getName();
                    int c = e.getValue();
                    return c > 1 ? c + " x " + name : name;
                })
                .collect(Collectors.joining(", "));
    }

    /** Untermenü Ansehen: Items im Raum und im Inventar betrachten. Liefert Array [bereich, auswahl].
     * bereich: 0=Zurück, 1=Raum-Item, 2=Inventar-Item. */
    public int[] drawExamineMenu(Node currentNode, List<Item> roomItems, List<Item> invItems) {
        clearScreen();
        System.out.println("================================================================================");
        System.out.println("  ANSEHEN in: " + currentNode.getName());
        System.out.println("================================================================================");
        System.out.println(" Was möchtest du dir ansehen?");
        System.out.println();
        System.out.println(" -- Im Raum --");
        if (roomItems.isEmpty()) {
            System.out.println("   (nichts hier)");
        }
        int idx = 1;
        for (Item i : roomItems) {
            System.out.println(" [" + idx++ + "] " + i.getName());
        }
        System.out.println(" -- Im Inventar --");
        if (invItems.isEmpty()) {
            System.out.println("   (leer)");
        }
        for (Item i : invItems) {
            System.out.println(" [" + idx++ + "] " + i.getName());
        }
        System.out.println(" [z] Zurück");
        System.out.println("--------------------------------------------------------------------------------");
        int choice = readChoice(" Auswahl > ");
        if (choice == 0 || choice == -1) return new int[]{0, 0};
        if (choice <= roomItems.size()) return new int[]{1, choice - 1};
        choice -= roomItems.size();
        if (choice <= invItems.size()) return new int[]{2, choice - 1};
        return new int[]{-1, 0}; // ungültig
    }

    /** Zeigt die Beschreibung eines Items an. */
    public void drawItemDescription(Item item) {
        clearScreen();
        System.out.println("================================================================================");
        System.out.println("  " + item.getName());
        System.out.println("================================================================================");
        String desc = item.getDescription();
        if (desc == null || desc.isEmpty()) {
            desc = "Ein unauffälliger Gegenstand. Du weißt nicht viel darüber.";
        }
        System.out.println(" " + desc);
        System.out.println("================================================================================");
        System.out.println();
        System.out.print(" (z oder Enter drücken, um zurückzukehren) > ");
        try {
            scanner.nextLine();
        } catch (Exception e) {
            // Eingabeende erreicht – ignorieren
        }
    }

    /** Untermenü Benutzen. Liefert 0=Zurück, 1..n=Auswahl. */
    public int drawUseMenu(List<Item> inv) {
        clearScreen();
        System.out.println("================================================================================");
        System.out.println("  BENUTZEN - Inventar");
        System.out.println("================================================================================");
        System.out.println(" Was möchtest du benutzen?");
        System.out.println();
        for (int i = 0; i < inv.size(); i++) {
            System.out.println(" [" + (i + 1) + "] " + inv.get(i).getName());
        }
        System.out.println(" [z] Zurück");
        System.out.println("--------------------------------------------------------------------------------");
        return readChoice(" Auswahl > ");
    }
}
