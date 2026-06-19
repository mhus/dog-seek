package com.example.doghunt.model;

import java.util.ArrayList;
import java.util.List;

public class GameMap {
    private List<Node> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private Node startNode;

    public void addNode(Node node) {
        nodes.add(node);
    }

    public void addEdge(Edge edge) {
        edges.add(edge);
    }

    public List<Node> getNodes() { return nodes; }
    public List<Edge> getEdges() { return edges; }

    public Node getStartNode() { return startNode; }
    public void setStartNode(Node startNode) { this.startNode = startNode; }
}
