package com.cha.model;

import com.cha.Direction;

import lombok.Data;

@Data
public class RankAndCount {
    private int count;
    private int rank;
    private int floor;
    private int maxAge;
    private Direction direction;
    public RankAndCount(int count, float rank, int floor, Direction direction, int maxAge) {
        this.count = count;
        this.rank = Math.round(rank*100);
        this.floor = floor;
        this.direction = direction;
    }

}
