package com.projectenigma.model;

/**
 * Dependency-free smoke test used when Gradle dependencies are not available.
 * Run with the root verify-logic script.
 */
public final class LogicSelfTest {
    private LogicSelfTest() {
    }

    public static void main(String[] args) {
        require(HeroClass.WARRIOR.displayName().equals("Sentinel"), "Sentinel display name mismatch");
        require(HeroClass.MAGE.displayName().equals("Hacker"), "Hacker display name mismatch");
        require(HeroClass.THIEF.displayName().equals("Sniper"), "Sniper display name mismatch");
        require(HeroClass.GRAPPLER.displayName().equals("Enforcer"), "Enforcer display name mismatch");
        require(HeroClass.CLERIC.displayName().equals("Bio-Medic"), "Bio-Medic display name mismatch");
        require(new Hero(HeroClass.MAGE).skillName().equals("system overload"),
                "Sci-fi tech skill mapping mismatch");

        DungeonGenerator generator = new DungeonGenerator();
        for (long seed = 1; seed <= 250; seed++) {
            DungeonMap map = generator.generate(61, 41, seed);
            require(map.shortestPathLength(map.start(), map.exit()) > 12,
                    "Exit was too close or unreachable for seed " + seed);
            int[][] distance = map.distanceMap(map.start());
            for (GridPoint point : map.walkableTiles()) {
                require(distance[point.x()][point.y()] >= 0,
                        "Disconnected tile " + point + " for seed " + seed);
            }
        }

        GameSession session = new GameSession(42L, HeroClass.MAGE);
        require(!session.enemies.isEmpty(), "A floor must contain enemies");
        require(!session.chests.isEmpty(), "A floor must contain chests");

        Hero hero = new Hero(HeroClass.WARRIOR);
        hero.attack = 999;
        DungeonEnemy enemy = new DungeonEnemy(99L, EnemyType.CAVE_SLIME, 1, 1, 1);
        TurnResult result = new BattleEngine(99L).resolve(hero, enemy, BattleAction.ATTACK);
        require(result.outcome() == BattleOutcome.VICTORY, "Lethal damage must produce victory");

        // The same engine, driven with two Heroes, is what PvP uses (see
        // com.projectenigma.network.PvPMatch and PvPMatchSmokeTest).
        Hero host = new Hero(HeroClass.WARRIOR);
        Hero guest = new Hero(HeroClass.MAGE);
        TurnResult pvpTurn = new BattleEngine(100L).resolve(host, guest, BattleAction.ATTACK);
        require(pvpTurn.actionAccepted(), "BattleEngine must resolve Hero-vs-Hero turns identically to Hero-vs-DungeonEnemy");

        System.out.println("LogicSelfTest passed: sci-fi roster, 250 connected dungeons, and battle/session/PvP-shape checks.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
