package com.example.doghunt.service;

import com.example.doghunt.model.*;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TerrainGenerator {

    private final PlacesService placesService;
    private final TerrainProvider terrainProvider;

    public TerrainGenerator(PlacesService placesService, TerrainProvider terrainProvider) {
        this.placesService = placesService;
        this.terrainProvider = terrainProvider;
    }

    @PostConstruct
    public void generateMap() {
        GameMap map = new GameMap();
        Map<String, Node> nodeById = new HashMap<>();

        // 1. Alle festen Orte laden
        List<PlaceTemplate> fixedPlaces = placesService.getPlacesByGroup("haus_fest");
        for (PlaceTemplate t : fixedPlaces) {
            Node n = new Node(t.getId(), t.getName(), t.getDescription());
            nodeById.put(t.getId(), n);
            map.addNode(n);
            if (t.getId().equals("eingang")) {
                map.setStartNode(n);
            }
        }

        // 2. Random Slots zuweisen
        Map<String, String> slotAssignments = new HashMap<>(); // Zuweisung slotGroup -> List<PlaceTemplate> (bereits gemischt und entnommen)
        Map<String, List<PlaceTemplate>> availableSlotsByGroup = new HashMap<>();

        // Alle festen Orte und ihre Verbindungen durchgehen
        for (PlaceTemplate template : fixedPlaces) {
            Node sourceNode = nodeById.get(template.getId());

            for (ConnectionTemplate conn : template.getConnections()) {
                if (conn.getTargetId() != null) {
                    // Direkte Verbindung (wird später in Schritt 3 verknüpft, weil noch nicht alle Random Nodes existieren könnten)
                } else if (conn.getSlotGroup() != null) {
                    // Random Slot auffüllen
                    String group = conn.getSlotGroup();
                    
                    if (!availableSlotsByGroup.containsKey(group)) {
                        List<PlaceTemplate> groupPlaces = new ArrayList<>(placesService.getPlacesByGroup(group));
                        Collections.shuffle(groupPlaces);
                        availableSlotsByGroup.put(group, groupPlaces);
                    }

                    List<PlaceTemplate> available = availableSlotsByGroup.get(group);
                    if (!available.isEmpty()) {
                        PlaceTemplate chosen = available.remove(0);
                        
                        Node targetNode = new Node(chosen.getId(), chosen.getName(), chosen.getDescription());
                        nodeById.put(chosen.getId(), targetNode);
                        map.addNode(targetNode);
                        
                        // Rückverbindung speichern, damit der Random-Raum auch zurück zum Flur kann
                        Edge forward = new Edge(conn.getEdgeName(), sourceNode, targetNode);
                        Edge backward = new Edge("Tür zurück", targetNode, sourceNode);
                        
                        map.addEdge(forward);
                        map.addEdge(backward);
                    }
                }
            }
        }

        // 3. Feste Verbindungen setzen
        // Zuerst sammeln wir alle expliziten Verbindungen (sourceId -> targetId),
        // damit wir keine doppelten Rückwege auto-generieren.
        Set<String> explicitConnections = new HashSet<>();
        for (PlaceTemplate t : fixedPlaces) {
            for (ConnectionTemplate c : t.getConnections()) {
                if (c.getTargetId() != null) {
                    explicitConnections.add(t.getId() + "->" + c.getTargetId());
                }
            }
        }

        for (PlaceTemplate template : fixedPlaces) {
            Node sourceNode = nodeById.get(template.getId());
            if (sourceNode == null) continue;

            for (ConnectionTemplate conn : template.getConnections()) {
                if (conn.getTargetId() != null) {
                    Node targetNode = nodeById.get(conn.getTargetId());
                    if (targetNode != null) {
                        Edge edge = new Edge(conn.getEdgeName(), sourceNode, targetNode);
                        if (conn.isLocked()) {
                            edge.setLocked(true);
                            edge.setRequiredKeyId(conn.getRequiredKeyId());
                        }
                        map.addEdge(edge);
                        
                        // Rückweg nur auto-erstellen, wenn im Template KEINE explizite
                        // Rückverbindung definiert ist (vermeidet duplizierte/umgehende Kanten,
                        // die z.B. ein Schloss an der Eingangstür aushebeln würden).
                        String reverseKey = conn.getTargetId() + "->" + sourceNode.getId();
                        if (!explicitConnections.contains(reverseKey) && !sourceNode.getId().equals("eingang")) {
                            Edge backEdge = new Edge("Zurück nach " + sourceNode.getName(), targetNode, sourceNode);
                            map.addEdge(backEdge);
                        }
                    }
                }
            }
        }

        // 3.5 Zufällige Räume abdunkeln
        Random random = new Random();
        List<Node> allNodes = map.getNodes();
        
        // Wir machen ~60% der Räume im Haus dunkel. Der Eingang (Start) sollte fairerweise immer hell sein.
        for (Node node : allNodes) {
            if (!node.getId().equals("eingang") && random.nextDouble() < 0.6) {
                node.setDark(true);
            }
        }

        // 4. Items entsprechend der Erlaubnisse verteilen

        for (ItemTemplate itemTpl : placesService.getAllItems()) {
            
            // Filtere erlaubte Nodes für dieses Item
            List<Node> validNodes = allNodes;
            if (itemTpl.getAllowedRooms() != null && !itemTpl.getAllowedRooms().isEmpty()) {
                validNodes = allNodes.stream()
                    .filter(n -> {
                        // Prüfen, ob die ID des Raums in den allowedRooms steht.
                        // Man könnte hier auch die Gruppe des Raums (eg_zimmer etc.) prüfen,
                        // aktuell machen wir es anhand der ID des Raums.
                        return itemTpl.getAllowedRooms().contains(n.getId());
                    })
                    .collect(Collectors.toList());
            }

            // Wenn es keine gültigen Nodes gibt, nimm alle als Fallback
            if (validNodes.isEmpty()) {
                validNodes = allNodes;
            }

            for (int i = 0; i < itemTpl.getTotalCount(); i++) {
                Node targetNode = validNodes.get(random.nextInt(validNodes.size()));
                Item item = new Item(itemTpl.getId(), itemTpl.getName());
                item.setDescription(itemTpl.getDescription());
                item.setDangerous(itemTpl.isDangerous());
                targetNode.getItems().add(item);
            }
        }

        terrainProvider.setMap(map);
        System.out.println("TerrainGenerator: Map generiert mit " + map.getNodes().size() + " Nodes und " + map.getEdges().size() + " Edges.");
    }
}
