package com.projectenigma.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public final class DungeonMap {
    private final int width;
    private final int height;
    private final TileType[][] tiles;
    private final List<Room> rooms;
    private GridPoint start;
    private GridPoint exit;

    public DungeonMap(int width, int height) {
        if (width < 15 || height < 15) {
            throw new IllegalArgumentException("Dungeon dimensions must be at least 15 x 15");
        }
        this.width = width;
        this.height = height;
        this.tiles = new TileType[width][height];
        this.rooms = new ArrayList<>();
        for (TileType[] column : tiles) {
            Arrays.fill(column, TileType.WALL);
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public TileType tileAt(int x, int y) {
        if (!isInside(x, y)) {
            return TileType.WALL;
        }
        return tiles[x][y];
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean isWalkable(int x, int y) {
        return isInside(x, y) && tiles[x][y] == TileType.FLOOR;
    }

    void carve(int x, int y) {
        if (x > 0 && y > 0 && x < width - 1 && y < height - 1) {
            tiles[x][y] = TileType.FLOOR;
        }
    }

    void addRoom(Room room) {
        rooms.add(room);
    }

    void setStart(GridPoint start) {
        this.start = start;
    }

    void setExit(GridPoint exit) {
        this.exit = exit;
    }

    public GridPoint start() {
        return start;
    }

    public GridPoint exit() {
        return exit;
    }

    public List<Room> rooms() {
        return Collections.unmodifiableList(rooms);
    }

    public List<GridPoint> walkableTiles() {
        List<GridPoint> result = new ArrayList<>();
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                if (isWalkable(x, y)) {
                    result.add(new GridPoint(x, y));
                }
            }
        }
        return result;
    }

    public int shortestPathLength(GridPoint from, GridPoint to) {
        if (from == null || to == null || !isWalkable(from.x(), from.y()) || !isWalkable(to.x(), to.y())) {
            return -1;
        }
        int[][] distance = distanceMap(from);
        return distance[to.x()][to.y()];
    }

    public int[][] distanceMap(GridPoint from) {
        int[][] distance = new int[width][height];
        for (int[] column : distance) {
            Arrays.fill(column, -1);
        }
        if (from == null || !isWalkable(from.x(), from.y())) {
            return distance;
        }

        Queue<GridPoint> queue = new ArrayDeque<>();
        queue.add(from);
        distance[from.x()][from.y()] = 0;
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!queue.isEmpty()) {
            GridPoint current = queue.remove();
            for (int i = 0; i < dx.length; i++) {
                int nx = current.x() + dx[i];
                int ny = current.y() + dy[i];
                if (isWalkable(nx, ny) && distance[nx][ny] == -1) {
                    distance[nx][ny] = distance[current.x()][current.y()] + 1;
                    queue.add(new GridPoint(nx, ny));
                }
            }
        }
        return distance;
    }

    public String toAscii() {
        StringBuilder output = new StringBuilder();
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                GridPoint point = new GridPoint(x, y);
                if (point.equals(start)) {
                    output.append('@');
                } else if (point.equals(exit)) {
                    output.append('>');
                } else {
                    output.append(tiles[x][y] == TileType.FLOOR ? '.' : '#');
                }
            }
            output.append('\n');
        }
        return output.toString();
    }
}
