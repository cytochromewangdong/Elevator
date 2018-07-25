package com.cha.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.cha.Consts;
import com.cha.Direction;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Lift {
    private Map<Integer, User> userSet = new HashMap<>();
    // private Set<User>[] angryBirdsPreserved = createPreservedTrail();

    // private Set<User>[] createPreservedTrail() {
    // @SuppressWarnings("unchecked")
    // Set<User>[] result = Utils.newArray(Set[].class, Consts.LEVEL_END + 1);
    // for (int i = Consts.LEVEL_START; i <= Consts.LEVEL_END; i++) {
    // result[i] = new HashSet<User>();
    // }
    // return result;
    //
    // }

    private Integer id;
    private Integer currentFloor;
    private Direction direction = Direction.DIRECTION_UNKNOWN;
    private Direction lastDirection = Direction.DIRECTION_UP;
    private boolean notMoved;
    private int handleCount;
    private int liftPolicy = Consts.POLICY_GREEDY;
    private boolean enable = true;
    // private Integer targetFloorForOptimization;
    // private FloorAndUser floorAndUser;

    public Direction getTrendDirection(GlobalState state) {
        Level level = state.getLevel(this.currentFloor);
        if (level.isEdge()) {
            return level.getOneDirection();
        } else {
            return direction == Direction.DIRECTION_UNKNOWN ? lastDirection : direction;
        }
    }

    public Integer getCurrentFloor() {
        return currentFloor;
    }

    public int getUserCount() {
        return userSet.size();
    }

    // public void reset() {
    // this.currentFloor = 1;
    // userSet.clear();
    // angryBirdsPreserved = createPreservedTrail();
    // }

    public Integer getId() {
        return id;
    }

    public Lift(int id) {
        this.id = id;
        currentFloor = 1;
    }

    public void goUp() {
        this.notMoved = false;
        currentFloor++;
    }

    public void goDown() {
        this.notMoved = false;
        currentFloor--;
    }

    public void loadPeopleForLevel(GlobalState state, Direction targetDirection) {
        Level level = state.getLevel(this.currentFloor);
        List<User> list = new ArrayList<>();
        List<User> userList = level.getDirectionListSortedByWaitTimeDesc(targetDirection);
        // TODO XXX
        int[] guess = new int[Consts.LEVEL_END + 1];
        for (int i = Consts.LEVEL_START; i <= Consts.LEVEL_END; i++) {
            // guess[i] = 19 - (Math.min(i, Consts.LEVEL_END - i) - 1) * 2;
            if (targetDirection == Direction.DIRECTION_UP) {
                guess[i] = 19 - (i-1);
            } else {
                guess[i] = 19;//- (Consts.LEVEL_END-i-1)*2 ;
            }
        }
        if (this.userSet.size() <= 0 && userList.size() <= (this.currentFloor == Consts.LEVEL_START ? 19 : 10) && this.liftPolicy == Consts.POLICY_ROUTINE) {// && this.liftPolicy == Consts.POLICY_ROUTINE
            boolean hasOld = false;
            for (User user : userList) {
                if (user.getWaitTime() > 26) {
                    hasOld = true;
                    break;
                }
            }
            if (!hasOld) {
                return;
            }
        }
        for (User user : userList) {
            if (this.canLoad(user, targetDirection)) {
                loadTheUser(user);
                list.add(user);
            }
        }
        level.updateLoadedPeople(list);
        // 仅仅有用户的时候才设置方向
        if (this.userSet.size() > 0) {
            this.direction = targetDirection;
        }
        // if (this.direction == Direction.DIRECTION_DOWN) {
        // this.direction = targetDirection;
        // }
    }

    public Direction reverseTrend(GlobalState state) {
        // if (level.isEdge())
        Level level = state.getLevel(this.currentFloor);
        assert (!level.isEdge());
        assert (this.direction == Direction.DIRECTION_DOWN);
        this.lastDirection = this.lastDirection.getReverse();
        return this.lastDirection;
    }
    // public void loadPeopleForLevel(GlobalState state) {
    //
    // Level level = state.getLevel(this.currentFloor);
    // List<User> list = new ArrayList<>();
    // // If the lift is idle, we should let it take as more as it could
    // if (direction == Direction.DIRECTION_UNKNOWN) {
    // List<User> userList = level.getEfficentList();
    // // TO add pretake User
    // // TO keep space for the angry birds which are preserved
    //
    // // Sort Desc
    // userList.stream().sorted((user1, user2) -> user2.getWaitTime() - user1.getWaitTime()).forEach(user -> {
    // // boolean excludeAngryBird = user.isAngryBird() && this.id.equals(user.getPreserved());
    // // should check the available space until that level
    // if (this.canLoad(user, direction)) {
    // loadTheUser(user);
    // list.add(user);
    // }
    // });
    //
    // if (userList.size() > 0) {
    // direction = userList.get(0).getDirection();
    // }
    // } else {
    // // Direction currentDirection = direction;
    // // if (direction.isWalkToEdge(this.currentFloor)) {
    // // // As for savior, there is something more important
    // // FloorAndUser floorAndUser = state.findTheOldestFloor();
    // // if (floorAndUser.getFloor() > 0 && floorAndUser.getUser().isAngryBird()) {
    // // int diff = floorAndUser.getFloor() - this.currentFloor;
    // // if (diff != 0) {
    // // this.floorAndUser = floorAndUser;
    // //
    // // this.direction = diff > 0 ? Direction.DIRECTION_UP : Direction.DIRECTION_DOWN;
    // // return;
    // // }
    // // }
    // // }
    // // currentDirection = currentDirection.getReverse();
    // // }
    //
    // List<User> userList = level.getDirectionListSortedByWaitTimeDesc(direction);
    // for (User user : userList) {
    // // boolean excludeAngryBird = user.isAngryBird() && this.id.equals(user.getPreserved());
    // if (this.canLoad(user, direction)) {
    // loadTheUser(user);
    // list.add(user);
    // }
    // }
    // if (direction.isWalkToEdge(this.currentFloor)) {
    // direction = this.userSet.isEmpty() ? Direction.DIRECTION_UNKNOWN : direction.getReverse();
    // }
    // }
    // level.removeLoadedPeople(list);
    // }

    private void loadTheUser(User user) {
        userSet.put(user.getId(), user);
        // this.angryBirdsPreserved[this.currentFloor].remove(user);
        // this.angryBirdsPreserved.remove(user);
        user.setLoadedLift(this.id);
    }

    public void resetDo() {
        notMoved = true;
    }

    // public void postDirectionAfterPeopleOnsite(GlobalState state) {
    // if (this.userSet.isEmpty()) {
    //
    // }
    // if (notMoved) {
    // // this.moveAndUnload(state);
    // }
    // }

    public int findBoundary() {
        if (this.getUserSet().size() == 0) {
            return 0;
        }

        // return this.direction == Direction.DIRECTION_DOWN ? Consts.LIFT_START : Consts.LIFT_END;
        if (this.liftPolicy == Consts.POLICY_ROUTINE) {
            return this.direction == Direction.DIRECTION_DOWN ? Consts.LIFT_START : Consts.LIFT_END;
        } else {
            int boundary = this.currentFloor;
            for (User user : this.getUserSet().values()) {
                if (user.getTheGoto() != null) {
                    boundary = this.direction == Direction.DIRECTION_DOWN ? Math.min(boundary, user.getTheGoto())
                            : Math.max(boundary, user.getTheGoto());
                } else {
                    // At least up or down a level if there is only direction but no togo
                    boundary =
                            this.direction == Direction.DIRECTION_DOWN ? this.currentFloor - 1 : this.currentFloor + 1;
                }
            }
            return boundary;
        }

    }

    public void updateTogo(Collection<User> gotoUserSet) {
        gotoUserSet.stream().forEach(user -> {
            User waitingUser = this.getUserSet().get(user.getId());
            if (waitingUser != null) {
                waitingUser.setTheGoto(user.getTheGoto());
            }
        });
    }

    public void doForDirection(Runnable up, Runnable down) {
        doForDirection(up, down, null);
    }

    public void doForDirection(Runnable up, Runnable down, Runnable unknown) {
        switch (direction) {
            case DIRECTION_UP:
                up.run();
                break;
            case DIRECTION_DOWN:
                down.run();
                break;
            case DIRECTION_UNKNOWN:
                if (unknown != null) {
                    unknown.run();
                }
                break;
        }

    }

    public void satisfyGuests(GlobalState state) {

        // if there is not any guests in the list, return
        if (this.userSet.isEmpty()) {
            return;
        }
        notMoved = false;
        doForDirection(this::goUp, this::goDown);
        // unload people
        List<User> arrivalUserList = userSet.values().stream().filter((user) -> currentFloor.equals(user.getTheGoto()))
                .collect(Collectors.toList());
        handleCount += arrivalUserList.size();
        arrivalUserList.forEach(user -> {
            userSet.remove(user.getId());
        });
        // 当前电梯里面用户数量如果归零，我们应该设置当前的电梯方面为自由。
        if (userSet.size() == 0) {
            // 如果是routine方式的电梯，我们保持默认方向
            // if (this.liftPolicy != Consts.POLICY_ROUTINE) {
            lastDirection = direction;
            direction = Direction.DIRECTION_UNKNOWN;
            // }

        }
        // it is a real unknown direction
        // if (userSet.size() == 0) {
        //// direction = Direction.DIRECTION_UNKNOWN;
        // if(direction == Direction.DIRECTION_UP)
        // {
        //
        // }
        // }


    }

    public boolean canLoad(User user, Direction direction) {
        if (this.isFull()) {
            return false;
        }
        // if (id.equals(user.getPreserved()) && user.isAngryBird()) {
        // return true;
        // }
        // keep space for angry birds
        // if (this.angryBirdsPreserved.size() > 0) {
        // if (direction == Direction.DIRECTION_UP) {
        // this.angryBirdsPreserved.
        // for (int i = this.currentFloor + 1; i <= boundary; i++) {
        // count = pretakeForOneLevel(state, count, i);
        // }
        // }
        // }
        return true;

    }

    public boolean isFull() {
        // return isAngrybird ? userSet.size() >= Consts.MAXIMUM_PEOPLE_PER_LIST
        // : userSet.size() + angryBirdsPreserved.size() >= Consts.MAXIMUM_PEOPLE_PER_LIST;
        return userSet.size() >= Consts.MAXIMUM_PEOPLE_PER_LIST;
    }

    public void markLiftUp(GlobalState state) {
        int boundary = this.findBoundary();
        int count = this.getUserCount();
        for (int i = this.currentFloor + 1; i <= boundary; i++) {
            count = pretakeForOneLevel(state, count, i);
        }
        // 如果是routine模式，标注所有的向下的楼层
        count = 0;
        if (this.liftPolicy == Consts.POLICY_ROUTINE) {
            int constDistance = Consts.LEVEL_END - this.currentFloor;
            for (int i = Consts.LEVEL_END; i <= Consts.LEVEL_START; i--) {
                count = markForLoopRoutine(state, count, i, Direction.DIRECTION_DOWN,
                        constDistance + Consts.LEVEL_END - i);
            }
        }
    }

    private int markForLoopRoutine(GlobalState state, int count, int predictFloor, Direction checkDirection,
            int distance) {
        Level predictLevel = state.getLevel(predictFloor);
        List<User> maybeUserList = predictLevel.getDirectionListSortedByWaitTimeDesc(checkDirection);
        count = doReserve(count, maybeUserList, distance);
        return count;
    }

    private int pretakeForOneLevel(GlobalState state, int count, int predictFloor) {

        int predictUnload = this.userSet.values().stream()
                .filter((user) -> user.getTheGoto() != null && user.getTheGoto() == predictFloor).mapToInt(e -> 1)
                .sum();
        count -= predictUnload;
        Level predictLevel = state.getLevel(predictFloor);
        List<User> maybeUserList = predictLevel.getDirectionListSortedByWaitTimeDesc(direction);
        int distance = Math.abs(predictFloor - this.currentFloor);
        count = doReserve(count, maybeUserList, distance);
        return count;
    }

    private int doReserve(int count, List<User> maybeUserList, int distance) {
        for (User user : maybeUserList) {
            if (count < Consts.MAXIMUM_PEOPLE_PER_LIST || Consts.notCheckForPretake) {
                if (user.makePreserve(this.id, distance)) {
                    // this.angryBirdsPreserved[predictFloor].add(user);
                    count++;
                }
            }
        }
        return count;
    }


    public void forEachUser(Consumer<User> consumer) {
        userSet.values().forEach(consumer);
    }

    public void markLiftDown(GlobalState state) {
        int boundary = this.findBoundary();
        int count = this.getUserCount();
        for (int i = this.currentFloor - 1; i >= boundary; i--) {
            count = pretakeForOneLevel(state, count, i);
        }

        // 如果是routine模式，标注所有的向下的楼层
        count = 0;
        if (this.liftPolicy == Consts.POLICY_ROUTINE) {
            int constDistance = this.currentFloor;
            for (int i = Consts.LEVEL_START; i <= Consts.LEVEL_END; i++) {
                count = markForLoopRoutine(state, count, i, Direction.DIRECTION_UP, constDistance + i);
            }
        }
    }


    public void clearPreviouseMark(GlobalState state) {
        for (int i = Consts.LEVEL_START; i <= Consts.LEVEL_END; i++) {
            // angryBirdsPreserved[i].clear();
            Level eachLevel = state.getLevel(i);
            eachLevel.removePretake(this);
        }
    }

    public void markLiftToTake(GlobalState state) {
        // 清空当前电梯预判数据
        clearPreviouseMark(state);
        // 每个电梯去预先标志当前数据
        doForDirection(() -> markLiftUp(state), () -> markLiftDown(state));
    }

    public void clearLoadedDataUp() {
        int boundary = this.findBoundary();
        for (int i = this.currentFloor + 1; i <= boundary; i++) {
            // angryBirdsPreserved[i].removeIf((user) -> user.getLoadedLift() != null);

        }
    }

    public void clearLoadedDataDown() {
        int boundary = this.findBoundary();
        for (int i = this.currentFloor - 1; i >= boundary; i--) {
            // angryBirdsPreserved[i].removeIf((user) -> user.getLoadedLift() != null);
        }
    }

    public void clearLoadedDataUnknown() {
        for (int i = Consts.LEVEL_START; i <= Consts.LEVEL_END; i++) {
            // angryBirdsPreserved[i].removeIf((user) -> user.getLoadedLift() != null);
        }
    }

    public void notifyPostAllLoad() {
        doForDirection(this::clearLoadedDataUp, this::clearLoadedDataDown, this::clearLoadedDataUnknown);

    }
}
