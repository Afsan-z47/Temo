package com.projectenigma.model;

/**
 * Common contract for anything that can take a turn in {@link BattleEngine}.
 * {@code Hero} implements this directly so the same engine resolves PvE
 * (Hero vs DungeonEnemy) and PvP (Hero vs Hero) turns without change.
 *
 * <p>BattleEngine never knows about Hero or DungeonEnemy directly -- it only
 * calls methods on this interface. That is what lets one engine serve
 * dungeon combat today and networked PvP (and later, co-op) without a
 * second combat implementation.
 */
public interface Combatant {

    /** Name shown in battle log messages, e.g. "Sentinel" or "Recon Drone". */
    String displayName();

    boolean isAlive();

    int health();

    int maxHealth();

    void setHealth(int health);

    int mana();

    int maxMana();

    void setMana(int mana);

    int attack();

    int defense();

    /** Percent chance (0-100) that this combatant's ATTACK/SKILL crits. */
    int criticalChance();

    /** Damage multiplier applied when this combatant uses SKILL. */
    float skillMultiplier();

    /** Flavor name of this combatant's SKILL action, e.g. "system overload". */
    String skillName();

    /** Percent chance (0-100) that this combatant's RUN action succeeds. */
    int escapeChance();

    int potions();

    /**
     * Attempts to consume one potion for the standard heal amount.
     *
     * @return {@code false} if no potions remain or health is already full;
     *         no state changes in that case.
     */
    boolean usePotion();

    /**
     * Whether this combatant chose GUARD on their last turn and has not yet
     * been struck since. BattleEngine halves incoming damage once, then
     * clears the flag on the defending combatant.
     */
    boolean isGuarding();

    void setGuarding(boolean guarding);
}
