package com.projectenigma.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionTest {
    @Test
    void generatedObjectsHaveValidDistinctPositions() {
        GameSession session = new GameSession(20260801L, HeroClass.WARRIOR);
        Set<GridPoint> occupied = new HashSet<>();
        occupied.add(session.dungeon().start());
        occupied.add(session.dungeon().exit());
        for (DungeonEnemy enemy : session.enemies) {
            GridPoint point = new GridPoint(enemy.x, enemy.y);
            assertTrue(session.dungeon().isWalkable(enemy.x, enemy.y));
            assertTrue(occupied.add(point));
        }
        for (DungeonChest chest : session.chests) {
            GridPoint point = new GridPoint(chest.x, chest.y);
            assertTrue(session.dungeon().isWalkable(chest.x, chest.y));
            assertTrue(occupied.add(point));
        }
        assertNotNull(session.enemyAt(session.enemies.get(0).x, session.enemies.get(0).y));
    }
}
