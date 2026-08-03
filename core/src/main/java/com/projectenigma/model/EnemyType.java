package com.projectenigma.model;

public enum EnemyType {
    // Enum constants stay unchanged so existing save files remain compatible.
    CAVE_SLIME("Recon Drone"),
    BONE_SENTINEL("Aegis Robot"),
    ABYSS_MAGE("Helix Cyborg"),
    FLOOR_WARDEN("Enhanced Warden");

    private final String displayName;

    EnemyType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
