package com.projectenigma.model;

public enum BattleAction {
    ATTACK("Attack", "Reliable physical damage"),
    SKILL("Tech Skill", "Stronger hit; costs 3 EN"),
    GUARD("Guard", "Halves the next enemy hit"),
    POTION("Potion", "Restore 35 HP"),
    RUN("Run", "Try to escape the encounter");

    private final String label;
    private final String description;

    BattleAction(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }
}
