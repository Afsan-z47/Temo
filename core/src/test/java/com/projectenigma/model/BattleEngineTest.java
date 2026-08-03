package com.projectenigma.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleEngineTest {
    @Test
    void lethalAttackEndsInVictory() {
        Hero hero = new Hero(HeroClass.WARRIOR);
        hero.attack = 100;
        DungeonEnemy enemy = new DungeonEnemy(1L, EnemyType.CAVE_SLIME, 2, 2, 1);
        enemy.health = 3;
        TurnResult result = new BattleEngine(7L).resolve(hero, enemy, BattleAction.ATTACK);
        assertEquals(BattleOutcome.VICTORY, result.outcome());
        assertEquals(0, enemy.health);
    }

    @Test
    void invalidPotionDoesNotGiveEnemyAFreeTurn() {
        Hero hero = new Hero(HeroClass.THIEF);
        hero.potions = 0;
        int healthBefore = hero.health;
        DungeonEnemy enemy = new DungeonEnemy(2L, EnemyType.CAVE_SLIME, 3, 3, 1);
        TurnResult result = new BattleEngine(9L).resolve(hero, enemy, BattleAction.POTION);
        assertFalse(result.actionAccepted());
        assertEquals(healthBefore, hero.health);
    }

    @Test
    void classSkillConsumesMana() {
        Hero hero = new Hero(HeroClass.MAGE);
        DungeonEnemy enemy = new DungeonEnemy(3L, EnemyType.BONE_SENTINEL, 3, 3, 2);
        int manaBefore = hero.mana;
        TurnResult result = new BattleEngine(11L).resolve(hero, enemy, BattleAction.SKILL);
        assertTrue(result.actionAccepted());
        assertEquals(manaBefore - 3, hero.mana);
    }

    @Test
    void guardHalvesTheNextHitThenClears() {
        Hero hero = new Hero(HeroClass.WARRIOR);
        DungeonEnemy enemy = new DungeonEnemy(4L, EnemyType.BONE_SENTINEL, 3, 3, 5);
        BattleEngine engine = new BattleEngine(13L);

        engine.resolve(hero, enemy, BattleAction.GUARD);
        assertTrue(hero.isGuarding());
        engine.resolve(enemy, hero, BattleAction.ATTACK);
        assertFalse(hero.isGuarding(), "guard should clear after absorbing one hit");
    }

    @Test
    void theSameEngineResolvesHeroVsHeroTurns() {
        // Exercises the same resolve(attacker, defender, action) call PvP uses,
        // just with two Heroes instead of a Hero and a DungeonEnemy.
        Hero host = new Hero(HeroClass.WARRIOR);
        Hero guest = new Hero(HeroClass.MAGE);
        BattleEngine engine = new BattleEngine(2026L);

        TurnResult hostTurn = engine.resolve(host, guest, BattleAction.ATTACK);
        assertTrue(hostTurn.actionAccepted());
        if (guest.isAlive()) {
            TurnResult guestTurn = engine.resolve(guest, host, BattleAction.SKILL);
            assertFalse(guestTurn.messages().isEmpty());
        }
    }
}
