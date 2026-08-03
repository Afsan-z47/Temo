package com.projectenigma.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameSession {
    public static final int DUNGEON_WIDTH = 61;
    public static final int DUNGEON_HEIGHT = 41;

    public long campaignSeed;
    public long floorSeed;
    public int floorNumber = 1;
    public Hero hero = new Hero();
    public int playerX;
    public int playerY;
    public int stepsTaken;
    public ArrayList<DungeonEnemy> enemies = new ArrayList<>();
    public ArrayList<DungeonChest> chests = new ArrayList<>();

    private transient DungeonMap dungeon;

    public GameSession() {
        // Required by libGDX Json.
    }

    public GameSession(long campaignSeed, HeroClass heroClass) {
        this.campaignSeed = campaignSeed;
        this.hero = new Hero(heroClass);
        beginFloor(1);
    }

    public DungeonMap dungeon() {
        if (dungeon == null) {
            rebuildTransientState();
        }
        return dungeon;
    }

    public void beginNextFloor() {
        beginFloor(floorNumber + 1);
        hero.restoreMana(Math.max(2, hero.maxMana / 3));
        hero.heal(Math.max(6, hero.maxHealth / 10));
    }

    private void beginFloor(int targetFloor) {
        floorNumber = Math.max(1, targetFloor);
        floorSeed = mixSeed(campaignSeed, floorNumber);
        dungeon = new DungeonGenerator().generate(DUNGEON_WIDTH, DUNGEON_HEIGHT, floorSeed);
        playerX = dungeon.start().x();
        playerY = dungeon.start().y();
        enemies.clear();
        chests.clear();
        populateFloor();
    }

    public void rebuildTransientState() {
        if (floorNumber < 1) {
            floorNumber = 1;
        }
        if (floorSeed == 0L) {
            floorSeed = mixSeed(campaignSeed, floorNumber);
        }
        dungeon = new DungeonGenerator().generate(DUNGEON_WIDTH, DUNGEON_HEIGHT, floorSeed);
        if (!dungeon.isWalkable(playerX, playerY)) {
            playerX = dungeon.start().x();
            playerY = dungeon.start().y();
        }
        if (enemies == null) {
            enemies = new ArrayList<>();
        }
        if (chests == null) {
            chests = new ArrayList<>();
        }
        enemies.removeIf(enemy -> enemy == null || !dungeon.isWalkable(enemy.x, enemy.y));
        chests.removeIf(chest -> chest == null || !dungeon.isWalkable(chest.x, chest.y));
    }

    private void populateFloor() {
        List<GridPoint> candidates = dungeon.walkableTiles();
        Random random = new Random(floorSeed ^ 0x6A09E667F3BCC909L);
        Collections.shuffle(candidates, random);
        Set<GridPoint> occupied = new HashSet<>();
        occupied.add(dungeon.start());
        occupied.add(dungeon.exit());

        int enemyTarget = Math.min(12, 4 + floorNumber);
        int enemyIndex = 0;
        for (GridPoint point : candidates) {
            if (enemyIndex >= enemyTarget) {
                break;
            }
            if (!validSpawn(point, occupied, 7, 3)) {
                continue;
            }
            EnemyType type = chooseEnemyType(random, enemyIndex);
            long id = mixSeed(floorSeed, enemyIndex + 1);
            enemies.add(new DungeonEnemy(id, type, point.x(), point.y(), floorNumber));
            occupied.add(point);
            enemyIndex++;
        }

        int chestTarget = Math.min(6, 2 + (floorNumber + 1) / 2);
        for (GridPoint point : candidates) {
            if (chests.size() >= chestTarget) {
                break;
            }
            if (validSpawn(point, occupied, 4, 2)) {
                chests.add(new DungeonChest(point.x(), point.y()));
                occupied.add(point);
            }
        }
    }

    private boolean validSpawn(GridPoint point, Set<GridPoint> occupied, int distanceFromStart, int distanceFromExit) {
        return !occupied.contains(point)
                && point.manhattanDistance(dungeon.start()) >= distanceFromStart
                && point.manhattanDistance(dungeon.exit()) >= distanceFromExit;
    }

    private EnemyType chooseEnemyType(Random random, int enemyIndex) {
        if (floorNumber % 5 == 0 && enemyIndex == 0) {
            return EnemyType.FLOOR_WARDEN;
        }
        int roll = random.nextInt(100);
        if (floorNumber >= 3 && roll < 25) {
            return EnemyType.ABYSS_MAGE;
        }
        if (floorNumber >= 2 && roll < 60) {
            return EnemyType.BONE_SENTINEL;
        }
        return EnemyType.CAVE_SLIME;
    }

    public boolean canMoveTo(int x, int y) {
        return dungeon().isWalkable(x, y);
    }

    public void movePlayerTo(int x, int y) {
        if (!canMoveTo(x, y)) {
            throw new IllegalArgumentException("Player cannot move onto a wall");
        }
        playerX = x;
        playerY = y;
        stepsTaken++;
    }

    public boolean isAtExit() {
        return playerX == dungeon().exit().x() && playerY == dungeon().exit().y();
    }

    public DungeonEnemy enemyAt(int x, int y) {
        for (DungeonEnemy enemy : enemies) {
            if (enemy.isAlive() && enemy.x == x && enemy.y == y) {
                return enemy;
            }
        }
        return null;
    }

    public DungeonChest chestAt(int x, int y) {
        for (DungeonChest chest : chests) {
            if (chest.x == x && chest.y == y) {
                return chest;
            }
        }
        return null;
    }

    public List<String> openChest(DungeonChest chest) {
        if (chest == null || chest.opened) {
            return List.of();
        }
        chest.opened = true;
        Random lootRandom = new Random(floorSeed ^ ((long) chest.x << 32) ^ chest.y);
        int goldFound = 8 + floorNumber * 3 + lootRandom.nextInt(12);
        hero.gold += goldFound;
        List<String> result = new ArrayList<>();
        result.add("Chest opened: +" + goldFound + " gold.");
        if (lootRandom.nextInt(100) < 55) {
            hero.potions++;
            result.add("You also found a potion.");
        }
        if (lootRandom.nextInt(100) < 35) {
            int restored = hero.restoreMana(3);
            if (restored > 0) {
                result.add("An energy cell restores " + restored + " EN.");
            }
        }
        return result;
    }

    public List<String> defeatEnemy(DungeonEnemy defeated) {
        List<String> result = new ArrayList<>();
        if (defeated == null) {
            return result;
        }
        int experience = defeated.experienceReward;
        int gold = defeated.goldReward;
        hero.gold += gold;
        result.add("Victory: +" + experience + " XP, +" + gold + " gold.");
        result.addAll(hero.gainExperience(experience));

        Random lootRandom = new Random(defeated.id ^ floorSeed);
        if (lootRandom.nextInt(100) < 28) {
            hero.potions++;
            result.add("The enemy dropped a potion.");
        }
        for (Iterator<DungeonEnemy> iterator = enemies.iterator(); iterator.hasNext();) {
            if (iterator.next().id == defeated.id) {
                iterator.remove();
                break;
            }
        }
        return result;
    }

    public static long mixSeed(long seed, int value) {
        long mixed = seed + 0x9E3779B97F4A7C15L * value;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }
}
