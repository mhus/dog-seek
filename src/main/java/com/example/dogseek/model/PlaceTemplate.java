package com.example.dogseek.model;

import java.util.ArrayList;
import java.util.List;

public class PlaceTemplate {
    private String id;
    private String name;
    private String description;
    private String group;
    private List<ConnectionTemplate> connections = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public List<ConnectionTemplate> getConnections() { return connections; }
    public void setConnections(List<ConnectionTemplate> connections) { this.connections = connections; }
}
