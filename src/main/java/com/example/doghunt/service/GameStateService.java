package com.example.doghunt.service;

import com.example.doghunt.model.PlayerState;
import org.springframework.stereotype.Service;

@Service
public class GameStateService {
    
    private PlayerState playerState;
    private String dogName = "Bello";

    public void init(PlayerState playerState) {
        this.playerState = playerState;
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    public String getDogName() {
        return dogName;
    }

    public void setDogName(String dogName) {
        this.dogName = dogName;
    }
}
