package com.projectenigma.model;

/** Immutable integer coordinate in dungeon tile space. */
public record GridPoint(int x, int y) {
    public int manhattanDistance(GridPoint other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }
}
