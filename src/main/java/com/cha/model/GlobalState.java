package com.cha.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.cha.Consts;
import com.cha.Direction;

public class GlobalState {

    public Level[] levelList = new Level[Consts.LEVEL_END + 1];
    public Lift[] liftList = new Lift[Consts.LIFT_END + 1];
    // add one more element to avoid trouble
    public int[] statisitcs = new int[Consts.LEVEL_END + 1];

    public int time = 0;

    public GlobalState() {
        reset();

    }

    public Level getLevel(int floor) {
        return levelList[floor];
    }

    public void beforeTimebeat() {
        time++;
        getLiftList().forEach(e -> {
            e.resetDo();
        });
    }

    private Stream<Lift> getLiftList() {

        return Arrays.stream(liftList).filter(lift -> lift != null && lift.isEnable());
    }

    public void updateLevelWithNewUsers(Collection<User> newUserSet) {
        if (newUserSet == null) {
            return;
        }
        newUserSet.stream().forEach(newUser -> {
            // count for the floor
            statisitcs[newUser.getCurrentFloor()] += 1;
            levelList[newUser.getCurrentFloor()].getUserSet().put(newUser.getId(), newUser);
        });

    }

    public void updateLiftWithGoto(Collection<User> gotoUserSet) {
        if (gotoUserSet == null) {
            return;
        }
        // We let the guests select their target floors.
        getLiftContainingGuests().forEach(lift -> {
            gotoUserSet.stream().forEach(user -> {
                User waitingUser = lift.getUserSet().get(user.getId());
                if (waitingUser != null) {
                    waitingUser.setTheGoto(user.getTheGoto());
                }
            });

        });
    }

    public void satisfyGuests() {
        getLiftContainingGuests().forEach(lift -> {
            lift.satisfyGuests(this);
        });
    }

    private Stream<Lift> getLiftContainingGuests() {
        return getLiftList().filter(lift -> lift.getUserCount() > 0);
    }

    private Stream<Lift> getEmptyLift() {
        return getLiftList().filter(lift -> lift.getUserCount() <= 0);
    }

    // public void redoIt() {
    // Arrays.stream(liftList).forEach(lift -> {
    // lift.postDirectionAfterPeopleOnsite(this);
    // });
    // }

    public void reset() {
        for (int i = Consts.LEVEL_START; i <= Consts.LEVEL_END; i++) {
            levelList[i] = new Level(i);
        }
        for (int i = Consts.LIFT_START; i <= Consts.LIFT_END; i++) {
            liftList[i] = new Lift(i);
        }
        // let the last one to be the savior
        // liftList[Consts.LIFT_END].setSavior(true);
        // Set the last one with policy routine
        liftList[Consts.LIFT_END].setLiftPolicy(Consts.POLICY_ROUTINE);
        liftList[2].setLiftPolicy(Consts.POLICY_ROUTINE);
//         liftList[1].setLiftPolicy(Consts.POLICY_ROUTINE);
        // liftList[2].setEnable(false);
        // liftList[1].setEnable(false);
        // liftList[2].setLiftPolicy(Consts.POLICY_ROUTINE);
        // liftList[1].setLiftPolicy(Consts.POLICY_ROUTINE);
        // statisitcs = new int[Consts.LEVEL_END];
        for (int i = 0; i < statisitcs.length; i++) {
            statisitcs[i] = 0;
        }
    }

    public void postTimeBeat() {
        getLevelList().forEach(level -> {
            level.addWaitTime();
        });

    }

    public void doPlan() {
        // Sorted by the floor desc
        getLiftList().forEach((lift) -> {
            lift.clearPreviouseMark(this);
        });
        // 对于每一个上升的电梯，我们找到最高的电梯做预算, 这样最高则会优先
        getLiftList().filter((lift) -> lift.getDirection() == Direction.DIRECTION_UP).sorted((lift1, lift2) -> {
            int difference = lift2.getCurrentFloor() - lift1.getCurrentFloor();
            if (difference == 0) {
                difference = lift1.getUserCount() - lift2.getUserCount();
            }
            return difference;
        }).forEachOrdered(lift -> {
            lift.markLiftToTake(this);
        });
        // 对于每一个下降的电梯，我们找到最低的电梯做运算，这样最低会优先
        getLiftList().filter((lift) -> lift.getDirection() == Direction.DIRECTION_DOWN).sorted((lift1, lift2) -> {
            int difference = lift1.getCurrentFloor() - lift2.getCurrentFloor();
            if (difference == 0) {
                difference = lift1.getUserCount() - lift2.getUserCount();
            }
            return difference;
        }).forEachOrdered(lift -> {

            lift.markLiftToTake(this);
        });

        this.getEmptyLift().filter((lift) -> lift.getLiftPolicy() == Consts.POLICY_ROUTINE)
                .sorted((l1, l2) -> l1.getCurrentFloor() - l2.getCurrentFloor()).forEachOrdered((lift) -> {
                    // 我们做计划之前，删除以前的计划
                    Level liftLevel = this.getLevel(lift.getCurrentFloor());
                    Direction trend = lift.getTrendDirection(this);
                    List<User> loadableUsers = liftLevel.getDirectionListSortedByWaitTimeDesc(trend);
                    // 有人, 我们需要载入人员
                    if (!loadableUsers.isEmpty()) {
                        lift.loadPeopleForLevel(this, trend);
                    } else {
                        if (trend == Direction.DIRECTION_UP) {
                            handleDirectionUp(lift);
                        } else {
                            handleDirectionDown(lift);
                        }
                    }
                    // }

                });
        this.getEmptyLift().filter((lift) -> lift.getLiftPolicy() == Consts.POLICY_GREEDY)
                .sorted((l1, l2) -> l1.getCurrentFloor() - l2.getCurrentFloor()).forEachOrdered((lift) -> {
                    handleGreedy(lift, false);
                });
        this.getEmptyLift().filter((lift) -> lift.getLiftPolicy() == Consts.POLICY_GREEDY)
                .sorted((l1, l2) -> l1.getCurrentFloor() - l2.getCurrentFloor()).forEachOrdered((lift) -> {
                    handleGreedy(lift, true);
                });

    }

    private void handleGreedy(Lift lift, boolean finalDo) {
        // 向下走估分
        // RankAndCount[] rankDownCount = new RankAndCount[Consts.LEVEL_END + 1];
        lift.clearPreviouseMark(this);
        List<RankAndCount> collectorDown = new ArrayList<>();
        // 往下走的打分
        for (int i = lift.getCurrentFloor(); i <= Consts.LEVEL_END; i++) {
            this.savePreserved();
            collectorDown.add(calculateRankAndScoreDown(lift, i));
            this.restorePreserved();
        }

        // List<RankAndCount> collectorUp = new ArrayList<>();
        for (int i = lift.getCurrentFloor(); i >= Consts.LEVEL_START; i--) {
            this.savePreserved();
            collectorDown.add(calculateRankAndScoreUp(lift, i));
            this.restorePreserved();
        }
        RankAndCount option = collectorDown.stream().max((r1, r2) -> r1.getRank() - r2.getRank()).get();
        // RankAndCount down = collectorDown.stream().max((r1, r2) -> r1.getRank() - r2.getRank()).get();
//        if (option.getRank() <= 0) {
//            Optional<RankAndCount> rightOne =
//                    collectorDown.stream().filter(r -> r.getCount() > 0).max((r1, r2) -> r1.getCount() - r2.getCount());
//            if (rightOne.isPresent()) {
//                option = rightOne.get();
//            }
//        }

        // RankAndCount option = null;
        // if (up.getRank() == down.getRank()) {
        // option = down;
        // if (option.getRank() <= 0) {
        // RankAndCount rank = null;
        // for (RankAndCount e : collectorUp) {
        // if (e.getCount() > 0) {
        // rank = e;
        // break;
        // }
        // }
        // if (rank == null) {
        // for (RankAndCount e : collectorDown) {
        // if (e.getCount() > 0) {
        // rank = e;
        // break;
        // }
        // }
        // }
        // if (rank != null) {
        // option = rank;
        // }
        // }
        // } else {
        // option = up.getRank() > down.getRank() ? up : down;
        // }
        if (option.getRank() <= 0 && option.getCount() <= 0) {
            // 啥都不要做了

        } else {
            if (option.getDirection() == Direction.DIRECTION_DOWN) {
                // 去做标记
                calculateRankAndScoreDown(lift, option.getFloor());
                Direction targetDirection = Direction.DIRECTION_DOWN;
                if (finalDo) {
                    executePlanForDirection(lift, option, targetDirection);
                }
            } else {
                // 去做标记
                calculateRankAndScoreUp(lift, option.getFloor());
                Direction targetDirection = Direction.DIRECTION_UP;
                if (finalDo) {
                    executePlanForDirection(lift, option, targetDirection);
                }
            }
        }
    }

    private void executePlanForDirection(Lift lift, RankAndCount option, Direction targetDirection) {
        if (option.getCount() <= 17 && option.getMaxAge() < 10) {
            return;
        }
        if (lift.getCurrentFloor() == option.getFloor()
                && !this.getLevel(option.getFloor()).getDirectionList(targetDirection).isEmpty()) {
            lift.loadPeopleForLevel(this, targetDirection);
        } else {
            // 没有移动过，移动一下把
            if (lift.isNotMoved()) {
                if (lift.getCurrentFloor() < option.getFloor()) {
                    lift.goUp();
                } else if (lift.getCurrentFloor() > option.getFloor()) {
                    lift.goDown();
                } else {
                    if (targetDirection == Direction.DIRECTION_UP) {
                        lift.goUp();
                    } else {
                        lift.goDown();
                    }
                }
                // 如果移到目标层，载人
                if (lift.getCurrentFloor() == option.getFloor()) {
                    lift.loadPeopleForLevel(this, targetDirection);
                }
            }
        }
    }

    private RankAndCount calculateRankAndScoreUp(Lift lift, int ifFloor) {
        int delta = lift.getCurrentFloor() - ifFloor;
        int count = 0;
        float rank = 0;
        Level tLevel = this.getLevel(ifFloor);
        if (tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_UP).isEmpty()
                && ifFloor != lift.getCurrentFloor()) {
            return new RankAndCount(count, rank, ifFloor, Direction.DIRECTION_UP, 0);
        }
        // for (int i = ifFloor; i <= lift.getCurrentFloor(); i++) { TODO
        int time = 0;
        for (int i = ifFloor; i <= Consts.LEVEL_END; i++) {
            tLevel = this.getLevel(i);
            List<User> maybeUserList = tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_UP);
            int distance = i - ifFloor + delta;
            for (User user : maybeUserList) {
                if (Consts.timeOldFirst) {
                    if (user.makePreserve(lift.getId(), distance)) {
                        rank = Math.max(user.getWaitTime(), rank);
                    }
                } else {
                    if (count < Consts.MAXIMUM_PEOPLE_PER_LIST || Consts.notCheckForPretake) {
                        if (user.makePreserve(lift.getId(), distance)) {
                            // lift.getAngryBirdsPreserved()[i].add(user);
                            rank += calRank(distance, user);
                            count++;
                            time = Math.max(time, user.getWaitTime());
                        }
                    }
                }
            }
        }
        // return count;
        return new RankAndCount(count, rank, ifFloor, Direction.DIRECTION_UP, time);
    }

    private double calRank(int distance, User user) {
        return (1.0 / (1 + Math.pow(distance, 1.8) * 0.3));// * (Math.pow(user.getWaitTime(), 0.2) + 1); // +
                                                           // user.getWaitTime()
                                                           // * 20;// / (1 +
        // Math.pow(distance, 1.8) * 0.3);// ;/ (1 +
        // Math.pow(distance, 1.1)
        // * 0.3);// *
        // (Math.pow(user.getWaitTime(), 0.2) + 1);
        // 1.0 / (1 + Math.pow(distance, 1.8) * 0.3);1.0 / (1 + Math.pow(distance, 1.8) * 0.3);
    }

    private RankAndCount calculateRankAndScoreDown(Lift lift, int ifFloor) {
        int delta = ifFloor - lift.getCurrentFloor();
        int count = 0;
        float rank = 0;
        Level tLevel = this.getLevel(ifFloor);
        if (tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_DOWN).isEmpty()
                && ifFloor != lift.getCurrentFloor()) {
            return new RankAndCount(count, rank, ifFloor, Direction.DIRECTION_DOWN, 0);
        }
        // for (int i = ifFloor; i >= lift.getCurrentFloor(); i--) TODO
        int time = 0;
        for (int i = ifFloor; i >= Consts.LEVEL_START; i--) {
            tLevel = this.getLevel(i);
            List<User> maybeUserList = tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_DOWN);
            int distance = ifFloor - i + delta;
            for (User user : maybeUserList) {
                if (count < Consts.MAXIMUM_PEOPLE_PER_LIST || Consts.notCheckForPretake) {
                    if (user.makePreserve(lift.getId(), distance)) {
                        // lift.getAngryBirdsPreserved()[i].add(user);
                        rank += calRank(distance, user);
                        count++;
                        time = Math.max(time, user.getWaitTime());
                    }
                }
            }
        }
        // return count;
        return new RankAndCount(count, rank, ifFloor, Direction.DIRECTION_DOWN, time);
    }

    private void handleDirectionDown(Lift lift) {
        boolean shouldKeep = markDownDownKeepRoutine(lift) > 0;
        if (!shouldKeep) {
            boolean shouldReverse = markDownReverseRoutine(lift) > 0;
            if (shouldReverse) {
                // 如果反转，
                Direction trend = lift.reverseTrend(this);
                Level level = this.getLevel(lift.getCurrentFloor());
                if (lift.isNotMoved()) {
                    if (level.getDirectionList(trend).size() <= 0) {
                        lift.goUp();
                    }
                }
                // 反转必须载入当前层的等候人
                lift.loadPeopleForLevel(this, trend);

            } else {
                // 前走不行，后走也不行，不如不走
            }
        } else {
            if (lift.isNotMoved()) {
                lift.goDown();
                if (this.getLevel(lift.getCurrentFloor()).isBottom()) {
                    lift.loadPeopleForLevel(this, Direction.DIRECTION_UP);
                } else {
                    lift.loadPeopleForLevel(this, Direction.DIRECTION_DOWN);
                }

            } else {
                // 既然已经走过了，不需要走了
            }
        }

    }

    private int markDownReverseRoutine(Lift lift) {
        int count = 0;
        for (int i = lift.getCurrentFloor(); i <= Consts.LEVEL_END; i++) {
            Level tLevel = this.getLevel(i);
            List<User> maybeUserList = tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_UP);
            int distance = i - lift.getCurrentFloor();
            count = tryPreserveUserList(lift, count, i, distance, maybeUserList);
        }

        for (int i = Consts.LEVEL_END; i > lift.getCurrentFloor(); i--) {
            Level tLevel = this.getLevel(i);
            List<User> maybeUserList = tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_DOWN);
            int distance = Consts.LEVEL_END - i + Consts.LEVEL_END - lift.getCurrentFloor();
            count = tryPreserveUserList(lift, count, i, distance, maybeUserList);
        }
        return count;
    }

    private int markDownDownKeepRoutine(Lift lift) {
        int count = 0;
        for (int i = lift.getCurrentFloor(); i >= Consts.LEVEL_START; i--) {
            Level tLevel = this.getLevel(i);
            List<User> maybeUserList = tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_DOWN);
            int distance = lift.getCurrentFloor() - i;
            count = tryPreserveUserList(lift, count, i, distance, maybeUserList);
        }

        for (int i = Consts.LEVEL_START; i < lift.getCurrentFloor(); i++) {
            Level tLevel = this.getLevel(i);
            List<User> maybeUserList = tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_UP);
            int distance = i + lift.getCurrentFloor();
            count = tryPreserveUserList(lift, count, i, distance, maybeUserList);
        }
        return count;
    }

    private int markUpUpKeepRoutine(Lift lift) {
        int count = 0;
        for (int i = lift.getCurrentFloor(); i <= Consts.LEVEL_END; i++) {
            Level tLevel = this.getLevel(i);
            List<User> maybeUserList = tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_UP);
            int distance = i - lift.getCurrentFloor();
            count = tryPreserveUserList(lift, count, i, distance, maybeUserList);
        }

        for (int i = Consts.LEVEL_END; i > lift.getCurrentFloor(); i--) {
            Level tLevel = this.getLevel(i);
            int distance = Consts.LEVEL_END - i + Consts.LEVEL_END - lift.getCurrentFloor();
            List<User> maybeUserList = tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_DOWN);
            count = tryPreserveUserList(lift, count, i, distance, maybeUserList);
        }
        return count;
    }

    private int tryPreserveUserList(Lift lift, int count, int i, int distance, List<User> maybeUserList) {
        for (User user : maybeUserList) {
            if (count < Consts.MAXIMUM_PEOPLE_PER_LIST || Consts.notCheckForPretake) {
                if (user.makePreserve(lift.getId(), distance)) {
                    // lift.getAngryBirdsPreserved()[i].add(user);
                    count++;
                }
            }
        }
        return count;
    }


    private int markUpReverseRoutine(Lift lift) {
        int count = 0;
        for (int i = lift.getCurrentFloor(); i >= Consts.LEVEL_START; i--) {
            Level tLevel = this.getLevel(i);
            List<User> maybeUserList = tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_DOWN);
            int distance = lift.getCurrentFloor() - i;
            count = tryPreserveUserList(lift, count, i, distance, maybeUserList);
        }

        for (int i = Consts.LEVEL_START; i < lift.getCurrentFloor(); i++) {
            Level tLevel = this.getLevel(i);
            List<User> maybeUserList = tLevel.getDirectionListSortedByWaitTimeDesc(Direction.DIRECTION_UP);
            int distance = i + lift.getCurrentFloor();
            count = tryPreserveUserList(lift, count, i, distance, maybeUserList);
        }
        return count;
    }

    private void handleDirectionUp(Lift lift) {
        boolean shouldKeep = markUpUpKeepRoutine(lift) > 0;
        if (!shouldKeep) {
            boolean shouldReverse = markUpReverseRoutine(lift) > 0;
            if (shouldReverse) {
                // 如果反转，
                Direction trend = lift.reverseTrend(this);
                Level level = this.getLevel(lift.getCurrentFloor());
                if (lift.isNotMoved()) {
                    if (level.getDirectionList(trend).size() <= 0) {
                        lift.goDown();
                    }
                }
                // 反转必须载入当前层的等候人
                lift.loadPeopleForLevel(this, trend);

            } else {
                // 前走不行，后走也不行，不如不走
            }
        } else {
            if (lift.isNotMoved()) {
                lift.goUp();
                if (this.getLevel(lift.getCurrentFloor()).isTop()) {
                    lift.loadPeopleForLevel(this, Direction.DIRECTION_DOWN);
                } else {
                    lift.loadPeopleForLevel(this, Direction.DIRECTION_UP);
                }
            } else {
                // 既然已经走过了，不需要走了
            }
        }
        // lift.happyPretake(this);
    }

    public void notifyPostAllLoad() {
        Arrays.stream(liftList).forEach((list) -> {
            list.notifyPostAllLoad();
        });
    }

    // public void load() {
    // Arrays.stream(liftList).forEach((list) -> {
    // list.loadPeopleForLevel(this, list.getDirection());
    // });
    //
    // }
    //
    // public FloorAndUser findTheOldestFloor() {
    // int currentFloor = 0;
    // int time = 0;
    // FloorAndUser floorAndUser = new FloorAndUser();
    // for (Level level : levelList) {
    // for (User user : level.getUserSet().values())
    // if (!user.isPreserved() && user.getWaitTime() > time) {
    // time = user.getWaitTime();
    // currentFloor = level.getId();
    // floorAndUser.setUser(user);
    // floorAndUser.setFloor(currentFloor);
    // }
    // }
    // return floorAndUser;
    //
    // }

    public void loadLiftContainingGuests() {
        getLiftContainingGuests().forEach(lift -> {
            lift.loadPeopleForLevel(this, lift.getDirection());
        });

    }

    public void savePreserved() {
        this.forEachLevel(level -> level.savePreserved());
    }

    public void restorePreserved() {
        this.forEachLevel(level -> level.restorePreserved());
    }

    public void forEachLevel(Consumer<Level> consumer) {
        getLevelList().forEach(consumer);
    }

    private Stream<Level> getLevelList() {
        return Arrays.stream(levelList).filter(level -> level != null);
    }
}
