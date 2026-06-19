package com.example.dogseek.service;

import com.example.dogseek.model.ConnectionTemplate;
import com.example.dogseek.model.GameConfig;
import com.example.dogseek.model.ItemTemplate;
import com.example.dogseek.model.PlaceTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlacesService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<PlaceTemplate> places = new ArrayList<>();
    private List<ItemTemplate> items = new ArrayList<>();
    private List<String> dogNames = new ArrayList<>();

    public PlacesService() {
    }

    @RegisterReflectionForBinding({GameConfig.class, PlaceTemplate.class, ConnectionTemplate.class, ItemTemplate.class})
    @PostConstruct
    public void init() {
        try {
            InputStream is = new ClassPathResource("places.json").getInputStream();
            GameConfig config = objectMapper.readValue(is, GameConfig.class);
            places = config.getPlaces() != null ? config.getPlaces() : new ArrayList<>();
            items = config.getItems() != null ? config.getItems() : new ArrayList<>();
            dogNames = config.getDogNames() != null ? config.getDogNames() : new ArrayList<>();
            System.out.println("PlacesService: Erfolgreich " + places.size() + " Orte, " + items.size() + " Items und " + dogNames.size() + " Hundenamen geladen.");
        } catch (Exception e) {
            System.err.println("Fehler beim Laden von places.json: " + e.getMessage());
        }
    }

    public List<PlaceTemplate> getAllPlaces() {
        return places;
    }

    public List<PlaceTemplate> getPlacesByGroup(String group) {
        return places.stream()
            .filter(p -> p.getGroup().equals(group))
            .collect(Collectors.toList());
    }

    public List<ItemTemplate> getAllItems() {
        return items;
    }

    public List<String> getDogNames() {
        return dogNames;
    }
}
