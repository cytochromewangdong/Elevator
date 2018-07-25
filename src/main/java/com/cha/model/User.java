package com.cha.model;

import com.cha.Consts;
import com.cha.Direction;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class User {
    private Integer id;
    private Integer currentFloor;
    private Direction direction;
    private Integer theGoto;
    private int waitTime = 0;
    private PreserveInfo preserveInfo;
    private PreserveInfo savedPreserveInfo;
    private Integer loadedLift;

    public void addWaitTime() {
        this.waitTime++;
    }

    public boolean isNewGuest() {
        return this.waitTime == 0;
    }

    public void savePreserved() {
        this.savedPreserveInfo = this.preserveInfo;
    }
    public PreserveInfo restorePreserved() {
        this.preserveInfo = this.savedPreserveInfo;
        return this.preserveInfo;
    }
    public boolean makePreserve(int liftId, int distance) {
        PreserveInfo preservedInfo = new PreserveInfo(liftId, distance);
        if (this.preserveInfo == null) {
            this.preserveInfo = preservedInfo;
            return true;
        } else {
            if (this.preserveInfo.getLift() == liftId) {
                return true;
            } else {
                if (this.preserveInfo.getDistance() > preservedInfo.getDistance()) {
                    this.preserveInfo = preservedInfo;
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isPreserved() {
        return this.preserveInfo != null;
    }

    public boolean isAngryBird() {
        return this.waitTime >= Consts.ANGRY_BIRD;
    }
}
