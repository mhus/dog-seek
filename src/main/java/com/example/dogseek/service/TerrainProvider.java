package com.example.dogseek.service;

import com.example.dogseek.model.GameMap;
import org.springframework.stereotype.Service;

@Service
public class TerrainProvider {
    
    private GameMap currentMap;

    public TerrainProvider() {
        // Erstmal leere Map initialisieren, damit die Engine nicht abstürzt
        this.currentMap = new GameMap();
    }

    public GameMap getMap() {
        return currentMap;
    }

    public void setMap(GameMap map) {
        this.currentMap = map;
    }
}
