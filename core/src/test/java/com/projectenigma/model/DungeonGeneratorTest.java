package com.projectenigma.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonGeneratorTest {
    private final DungeonGenerator generator = new DungeonGenerator();

    @Test
    void sameSeedProducesSameDungeon() {
        DungeonMap first = generator.generate(61, 41, 123456789L);
        DungeonMap second = generator.generate(61, 41, 123456789L);
        assertEquals(first.toAscii(), second.toAscii());
    }

    @Test
    void differentSeedsProduceDifferentDungeons() {
        DungeonMap first = generator.generate(61, 41, 100L);
        DungeonMap second = generator.generate(61, 41, 200L);
        assertNotEquals(first.toAscii(), second.toAscii());
    }

    @Test
    void everyFloorTileIsReachableAndBorderStaysClosed() {
        for (long seed = 1; seed <= 40; seed++) {
            DungeonMap map = generator.generate(61, 41, seed);
            int[][] distances = map.distanceMap(map.start());
            for (GridPoint point : map.walkableTiles()) {
                assertTrue(distances[point.x()][point.y()] >= 0,
                        "Disconnected floor at " + point + " for seed " + seed);
            }
            for (int x = 0; x < map.width(); x++) {
                assertEquals(TileType.WALL, map.tileAt(x, 0));
                assertEquals(TileType.WALL, map.tileAt(x, map.height() - 1));
            }
            for (int y = 0; y < map.height(); y++) {
                assertEquals(TileType.WALL, map.tileAt(0, y));
                assertEquals(TileType.WALL, map.tileAt(map.width() - 1, y));
            }
            assertTrue(map.shortestPathLength(map.start(), map.exit()) > 12);
        }
    }
}
