package com.example.nidar.heatmap.model;

// heatmap/model/IncidentType.java
public enum IncidentType {
    SOS_TRIGGER(100),
    PHYSICAL_HARASSMENT(80),
    STALKING(75),
    SUSPICIOUS_ACTIVITY(40),
    POOR_LIGHTING(15),
    DESERTED_AREA(15);

    private final int baseWeight;

    IncidentType(int baseWeight) { this.baseWeight = baseWeight; }
    public int getBaseWeight() { return baseWeight; }
}