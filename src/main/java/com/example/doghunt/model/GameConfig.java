package com.example.doghunt.model;

import java.util.List;

public class GameConfig {
    private List<PlaceTemplate> places;
    private List<ItemTemplate> items;
    private List<String> dogNames;

    public List<PlaceTemplate> getPlaces() { return places; }
    public void setPlaces(List<PlaceTemplate> places) { this.places = places; }

    public List<ItemTemplate> getItems() { return items; }
    public void setItems(List<ItemTemplate> items) { this.items = items; }

    public List<String> getDogNames() { return dogNames; }
    public void setDogNames(List<String> dogNames) { this.dogNames = dogNames; }
}
