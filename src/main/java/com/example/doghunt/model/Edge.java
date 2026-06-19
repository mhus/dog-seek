package com.example.doghunt.model;

public class Edge {
    private String name; // z.B. "Tür", "Fenster", "Feuerleiter"
    private Node source;
    private Node target;

    private boolean locked;
    private String requiredKeyId;
    private boolean attempted; // true, sobald der Spieler einmal probiert hat, die Tür zu öffnen

    public Edge(String name, Node source, Node target) {
        this.name = name;
        this.source = source;
        this.target = target;
        this.locked = false;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Node getSource() { return source; }
    public void setSource(Node source) { this.source = source; }

    public Node getTarget() { return target; }
    public void setTarget(Node target) { this.target = target; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getRequiredKeyId() { return requiredKeyId; }
    public void setRequiredKeyId(String requiredKeyId) { this.requiredKeyId = requiredKeyId; }

    public boolean isAttempted() { return attempted; }
    public void setAttempted(boolean attempted) { this.attempted = attempted; }
}
