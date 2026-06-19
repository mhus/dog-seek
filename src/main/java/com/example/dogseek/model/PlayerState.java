package com.example.dogseek.model;

public class PlayerState {
    private Node currentNode;
    private java.util.List<Item> inventory = new java.util.ArrayList<>();
    private int lives = 2;

    public PlayerState(Node startNode) {
        this.currentNode = startNode;
    }

    public Node getCurrentNode() { return currentNode; }
    public void setCurrentNode(Node currentNode) { this.currentNode = currentNode; }

    public java.util.List<Item> getInventory() { return inventory; }
    public void setInventory(java.util.List<Item> inventory) { this.inventory = inventory; }

    public int getLives() { return lives; }
    public void setLives(int lives) { this.lives = lives; }
}
