package com.example.dogseek.model;

public class ConnectionTemplate {
    private String targetId;
    private String slotGroup;
    private String edgeName;
    private boolean locked;
    private String requiredKeyId;

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getSlotGroup() { return slotGroup; }
    public void setSlotGroup(String slotGroup) { this.slotGroup = slotGroup; }

    public String getEdgeName() { return edgeName; }
    public void setEdgeName(String edgeName) { this.edgeName = edgeName; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getRequiredKeyId() { return requiredKeyId; }
    public void setRequiredKeyId(String requiredKeyId) { this.requiredKeyId = requiredKeyId; }
}
