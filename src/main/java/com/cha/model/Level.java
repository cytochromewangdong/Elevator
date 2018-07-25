package com.cha.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.cha.Consts;
import com.cha.Direction;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = {"id"})
public class Level {

    private Map<Integer, User> userSet = new HashMap<>();
    private Integer id;

    private Direction oneDirection = null;

    public Level(int id) {
        this.id = id;
        if (this.isTop()) {
            oneDirection = Direction.DIRECTION_DOWN;
        } else if (this.isBottom()) {
            oneDirection = Direction.DIRECTION_UP;
        }
    }

    public void savePreserved() {
        this.forEachUser(user->user.savePreserved());
    }
    public void restorePreserved() {
        this.forEachUser(user->user.restorePreserved());
    }
    
    public boolean isEdge() {
        return isTop() || isBottom();
    }

    public boolean isTop() {
        return id == Consts.LEVEL_END;
    }

    public boolean isBottom() {
        return id == Consts.LEVEL_START;
    }

    public boolean hasPeople() {
        return !userSet.isEmpty();
    }

    public int getMaxNumber() {
        int up = getDirectionList(Direction.DIRECTION_UP).size();
        int down = getDirectionList(Direction.DIRECTION_DOWN).size();
        return Math.max(up, down);
    }

    public List<User> getEfficentList() {
        List<User> n = new ArrayList<>();
        List<User> p = new ArrayList<>();
        for (User user : userSet.values()) {
            if (user.getDirection() == Direction.DIRECTION_DOWN) {
                p.add(user);
            } else {
                n.add(user);
            }
        }
        List<User> result = n.size() > p.size() ? n : p;
        return result;
    }

    public List<User> getDirectionList(Direction direction) {
        return getDirectionStream(direction).collect(Collectors.toList());
    }

    public List<User> getDirectionListSortedByWaitTimeDesc(Direction direction) {
        return getDirectionStream(direction).sorted((user1, user2) -> user2.getWaitTime() - user1.getWaitTime())
                .collect(Collectors.toList());
    }

    private Stream<User> getDirectionStream(Direction direction) {
        return userSet.values().stream().filter(user -> user.getDirection() == direction);
    }

    public void updateLoadedPeople(List<User> userList) {
        userList.stream().forEach(user -> {
            userSet.remove(user.getId());
        });
    }

    public int countNotPreserved() {
        return userSet.values().stream().filter(user -> !user.isPreserved()).mapToInt(e -> 1).sum();
    }

    public void forEachUser(Consumer<User> consumer) {
        userSet.values().forEach(consumer);
    }

    public void addWaitTime() {
        forEachUser(user -> {
            user.addWaitTime();
        });
    }

    public void removePretake(Lift lift) {
        forEachUser(user -> {
            PreserveInfo info = user.getPreserveInfo();
            if (info != null && info.getLift() == lift.getId()) {
                user.setPreserveInfo(null);
            }
        });
    }
}
