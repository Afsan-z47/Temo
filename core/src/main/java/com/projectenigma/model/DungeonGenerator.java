package com.projectenigma.model;

import java.util.List;
import java.util.Random;

/**
 * Deterministic room-and-corridor generator. Each accepted room is connected
 * immediately, so every carved tile belongs to one reachable dungeon.
 */
public final class DungeonGenerator {
    private static final int ROOM_ATTEMPTS = 240;
    private static final int MAX_ROOMS = 18;

    public DungeonMap generate(int width, int height, long seed) {
        DungeonMap map = new DungeonMap(makeOdd(width), makeOdd(height));
        Random random = new Random(seed);

        for (int attempt = 0; attempt < ROOM_ATTEMPTS && map.rooms().size() < MAX_ROOMS; attempt++) {
            int roomWidth = randomOdd(random, 5, 11);
            int roomHeight = randomOdd(random, 5, 9);
            if (roomWidth >= map.width() - 2 || roomHeight >= map.height() - 2) {
                continue;
            }

            int x = 1 + random.nextInt(map.width() - roomWidth - 1);
            int y = 1 + random.nextInt(map.height() - roomHeight - 1);
            Room candidate = new Room(x, y, roomWidth, roomHeight);
            if (overlapsExisting(candidate, map.rooms())) {
                continue;
            }

            carveRoom(map, candidate);
            if (!map.rooms().isEmpty()) {
                GridPoint previous = map.rooms().get(map.rooms().size() - 1).center();
                connect(map, previous, candidate.center(), random.nextBoolean());
            }
            map.addRoom(candidate);
        }

        ensureMinimumLayout(map, random);
        addLoops(map, random);
        map.setStart(map.rooms().get(0).center());
        map.setExit(findFarthestTile(map, map.start()));
        return map;
    }

    private static int makeOdd(int value) {
        return value % 2 == 0 ? value - 1 : value;
    }

    private static int randomOdd(Random random, int minimum, int maximumInclusive) {
        int count = ((maximumInclusive - minimum) / 2) + 1;
        return minimum + random.nextInt(count) * 2;
    }

    private static boolean overlapsExisting(Room candidate, List<Room> rooms) {
        for (Room room : rooms) {
            if (candidate.intersectsWithMargin(room, 1)) {
                return true;
            }
        }
        return false;
    }

    private static void carveRoom(DungeonMap map, Room room) {
        for (int x = room.x(); x < room.right(); x++) {
            for (int y = room.y(); y < room.top(); y++) {
                map.carve(x, y);
            }
        }
    }

    private static void connect(DungeonMap map, GridPoint a, GridPoint b, boolean horizontalFirst) {
        if (horizontalFirst) {
            carveHorizontal(map, a.x(), b.x(), a.y());
            carveVertical(map, a.y(), b.y(), b.x());
        } else {
            carveVertical(map, a.y(), b.y(), a.x());
            carveHorizontal(map, a.x(), b.x(), b.y());
        }
    }

    private static void carveHorizontal(DungeonMap map, int fromX, int toX, int y) {
        int start = Math.min(fromX, toX);
        int end = Math.max(fromX, toX);
        for (int x = start; x <= end; x++) {
            map.carve(x, y);
        }
    }

    private static void carveVertical(DungeonMap map, int fromY, int toY, int x) {
        int start = Math.min(fromY, toY);
        int end = Math.max(fromY, toY);
        for (int y = start; y <= end; y++) {
            map.carve(x, y);
        }
    }

    private static void ensureMinimumLayout(DungeonMap map, Random random) {
        if (map.rooms().size() >= 2) {
            return;
        }

        Room first = new Room(2, 2, 7, 7);
        Room second = new Room(map.width() - 10, map.height() - 10, 7, 7);
        if (map.rooms().isEmpty()) {
            carveRoom(map, first);
            map.addRoom(first);
        }
        carveRoom(map, second);
        connect(map, map.rooms().get(0).center(), second.center(), random.nextBoolean());
        map.addRoom(second);
    }

    private static void addLoops(DungeonMap map, Random random) {
        int loopCount = Math.max(1, map.rooms().size() / 4);
        for (int i = 0; i < loopCount; i++) {
            Room first = map.rooms().get(random.nextInt(map.rooms().size()));
            Room second = map.rooms().get(random.nextInt(map.rooms().size()));
            if (first != second) {
                connect(map, first.center(), second.center(), random.nextBoolean());
            }
        }
    }

    private static GridPoint findFarthestTile(DungeonMap map, GridPoint start) {
        int[][] distance = map.distanceMap(start);
        GridPoint farthest = start;
        int farthestDistance = 0;
        for (int x = 1; x < map.width() - 1; x++) {
            for (int y = 1; y < map.height() - 1; y++) {
                if (distance[x][y] > farthestDistance) {
                    farthest = new GridPoint(x, y);
                    farthestDistance = distance[x][y];
                }
            }
        }
        return farthest;
    }
}
