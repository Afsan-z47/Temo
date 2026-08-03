package com.projectenigma.model;

public enum HeroClass {
    // Enum IDs stay unchanged so old save files and the existing sprite paths remain compatible.
    WARRIOR("Sentinel", "Armored frontline", 120, 8, 16, 7),
    MAGE("Hacker", "High-energy systems", 88, 16, 13, 3),
    THIEF("Sniper", "Precision criticals", 98, 11, 15, 4),
    GRAPPLER("Enforcer", "Close-range power", 110, 8, 18, 4),
    CLERIC("Bio-Medic", "Balanced field support", 104, 16, 12, 6);

    private final String displayName;
    private final String description;
    private final int health;
    private final int mana;
    private final int attack;
    private final int defense;

    HeroClass(String displayName, String description, int health, int mana, int attack, int defense) {
        this.displayName = displayName;
        this.description = description;
        this.health = health;
        this.mana = mana;
        this.attack = attack;
        this.defense = defense;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public int health() {
        return health;
    }

    public int mana() {
        return mana;
    }

    public int attack() {
        return attack;
    }

    public int defense() {
        return defense;
    }
}
