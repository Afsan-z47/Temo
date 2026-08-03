package com.projectenigma.model;

import java.util.ArrayList;
import java.util.List;

public class Hero implements Combatant {
    public HeroClass heroClass = HeroClass.WARRIOR;
    public int level = 1;
    public int experience = 0;
    public int maxHealth = 120;
    public int health = 120;
    public int maxMana = 8;
    public int mana = 8;
    public int attack = 16;
    public int defense = 7;
    public int potions = 3;
    public int gold = 0;

    /** Combat-only state. Never persisted -- a fresh guard should never survive a save/load. */
    public transient boolean guarding = false;

    public Hero() {
        // Required by libGDX Json.
    }

    public Hero(HeroClass heroClass) {
        this.heroClass = heroClass;
        this.maxHealth = heroClass.health();
        this.health = maxHealth;
        this.maxMana = heroClass.mana();
        this.mana = maxMana;
        this.attack = heroClass.attack();
        this.defense = heroClass.defense();
    }

    public int experienceForNextLevel() {
        return 45 + (level - 1) * 30;
    }

    public int heal(int amount) {
        int before = health;
        health = Math.min(maxHealth, health + Math.max(0, amount));
        return health - before;
    }

    public int restoreMana(int amount) {
        int before = mana;
        mana = Math.min(maxMana, mana + Math.max(0, amount));
        return mana - before;
    }

    public List<String> gainExperience(int amount) {
        List<String> messages = new ArrayList<>();
        experience += Math.max(0, amount);
        while (experience >= experienceForNextLevel()) {
            experience -= experienceForNextLevel();
            level++;
            int healthIncrease = heroClass == HeroClass.WARRIOR || heroClass == HeroClass.GRAPPLER ? 14 : 10;
            int manaIncrease = heroClass == HeroClass.MAGE || heroClass == HeroClass.CLERIC ? 4 : 2;
            maxHealth += healthIncrease;
            maxMana += manaIncrease;
            attack += heroClass == HeroClass.THIEF || heroClass == HeroClass.GRAPPLER ? 4 : 3;
            defense += heroClass == HeroClass.WARRIOR ? 3 : 2;
            health = maxHealth;
            mana = maxMana;
            messages.add("Level up! You are now level " + level + ".");
        }
        return messages;
    }

    public boolean usePotion() {
        if (potions <= 0 || health >= maxHealth) {
            return false;
        }
        potions--;
        heal(BattleEngine.potionHealAmount());
        return true;
    }

    public boolean isAlive() {
        return health > 0;
    }

    // ---- Combatant ----

    @Override
    public String displayName() {
        return heroClass.displayName();
    }

    @Override
    public int health() {
        return health;
    }

    @Override
    public int maxHealth() {
        return maxHealth;
    }

    @Override
    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
    }

    @Override
    public int mana() {
        return mana;
    }

    @Override
    public int maxMana() {
        return maxMana;
    }

    @Override
    public void setMana(int mana) {
        this.mana = Math.max(0, Math.min(maxMana, mana));
    }

    @Override
    public int attack() {
        return attack;
    }

    @Override
    public int defense() {
        return defense;
    }

    @Override
    public int criticalChance() {
        return switch (heroClass) {
            case WARRIOR -> 10;
            case MAGE -> 12;
            case THIEF -> 24;
            case GRAPPLER -> 16;
            case CLERIC -> 12;
        };
    }

    @Override
    public float skillMultiplier() {
        return switch (heroClass) {
            case WARRIOR -> 1.55f;
            case MAGE -> 1.85f;
            case THIEF -> 1.65f;
            case GRAPPLER -> 1.75f;
            case CLERIC -> 1.6f;
        };
    }

    @Override
    public String skillName() {
        return switch (heroClass) {
            case WARRIOR -> "breach pulse";
            case MAGE -> "system overload";
            case THIEF -> "deadeye shot";
            case GRAPPLER -> "kinetic slam";
            case CLERIC -> "nano disruptor";
        };
    }

    @Override
    public int escapeChance() {
        return Math.min(75, 38 + Math.max(0, level - 1) * 3);
    }

    @Override
    public int potions() {
        return potions;
    }

    @Override
    public boolean isGuarding() {
        return guarding;
    }

    @Override
    public void setGuarding(boolean guarding) {
        this.guarding = guarding;
    }
}
