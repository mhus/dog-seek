package com.example.doghunt.model;

public class ItemTemplate {
    private String id;
    private String name;
    private String description;
    private int totalCount;
    private java.util.List<String> allowedRooms; // Erlaubte Gruppen oder IDs
    private boolean dangerous; // Gefährlich: beim Aufheben verliert man ein Leben

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public java.util.List<String> getAllowedRooms() { return allowedRooms; }
    public void setAllowedRooms(java.util.List<String> allowedRooms) { this.allowedRooms = allowedRooms; }

    public boolean isDangerous() { return dangerous; }
    public void setDangerous(boolean dangerous) { this.dangerous = dangerous; }
}
