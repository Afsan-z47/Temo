package com.projectenigma.model;

public class DungeonChest {
    public int x;
    public int y;
    public boolean opened;

    public DungeonChest() {
        // Required by libGDX Json.
    }

    public DungeonChest(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
