package com.example.dogseek.model;

import java.util.List;

public class Node {
    private String id;
    private String name;
    private String description;
    private boolean isDark;
    private List<Item> items = new java.util.ArrayList<>();
    private int searchCount = 0; // wie oft in diesem Besuch gesucht wurde

    public Node(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isDark = false; // Standardmäßig hell
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isDark() { return isDark; }
    public void setDark(boolean dark) { isDark = dark; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public int getSearchCount() { return searchCount; }
    public void setSearchCount(int searchCount) { this.searchCount = searchCount; }
}
