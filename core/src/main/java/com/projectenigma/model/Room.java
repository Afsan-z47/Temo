package com.projectenigma.model;

/** A rectangular room. The maximum coordinates are exclusive. */
public record Room(int x, int y, int width, int height) {
    public int right() {
        return x + width;
    }

    public int top() {
        return y + height;
    }

    public GridPoint center() {
        return new GridPoint(x + width / 2, y + height / 2);
    }

    public boolean intersectsWithMargin(Room other, int margin) {
        return x - margin < other.right()
                && right() + margin > other.x()
                && y - margin < other.top()
                && top() + margin > other.y();
    }
}
