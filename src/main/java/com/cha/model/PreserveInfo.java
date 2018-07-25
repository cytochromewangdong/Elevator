package com.cha.model;

import lombok.Data;

@Data
public class PreserveInfo {
    // public static enum PreserveType {
    // Strong, Weak;
    // }

    private int lift;
    // private PreserveType type = PreserveType.Strong;
    private int distance;

    public PreserveInfo(int lift, int distance) {
        this.lift = lift;

        this.distance = distance;
    }
}
