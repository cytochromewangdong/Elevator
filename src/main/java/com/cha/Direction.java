package com.cha;

import java.util.HashMap;
import java.util.Map;

public enum Direction {
    DIRECTION_UNKNOWN(-1), DIRECTION_DOWN(0), DIRECTION_UP(1);

    private int value;
    private static Map<Integer, Direction> map = new HashMap<>();
    private static int[] edge = new int[] {Consts.LEVEL_START, Consts.LEVEL_END};

    private static Direction[] reverse = new Direction[] {DIRECTION_UP, DIRECTION_DOWN};

    private Direction(int value) {
        this.value = value;
    }

    static {
        for (Direction pageType : Direction.values()) {
            map.put(pageType.value, pageType);
        }
    }

    public static Direction valueOf(int pageType) {
        return (Direction) map.get(pageType);
    }

    public int getValue() {
        return value;
    }

    public boolean isWalkToEdge(int floor) {
        if (this.value < 0) {
            return false;
        }
        return edge[this.value] == floor;
    }

    public Direction getReverse() {
        if (this.value < 0) {
            return this;
        }
        return reverse[this.value];
    }
}
