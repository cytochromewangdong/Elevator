package com.cha.service;

import java.util.ArrayList;
import java.util.List;

import com.cha.Direction;
import com.cha.model.RequestCom;
import com.cha.model.User;

public class Test {

    public static void moveTime(ChaService service, int times) {
        for (int i = 0; i < times; i++) {
            RequestCom com = new RequestCom();
            com = new RequestCom();

            com.setGoToList(newUserList);
            service.handleWorkLoad(com);
        }
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ChaService service = new ChaService();
        service.state.liftList[3].setCurrentFloor(11);
        // service.state.liftList[3].setLastDirection(Direction.d);
        service.state.liftList[2].setCurrentFloor(5);
        service.state.liftList[1].setCurrentFloor(5);
        RequestCom com = new RequestCom();
        service.handleWorkLoad(com);
        com = new RequestCom();
        List<User> newUserList = new ArrayList<>();
        User user = createUser(5, Direction.DIRECTION_UP, 9);
        newUserList.add(user);
        user = createUser(5, Direction.DIRECTION_UP, 9);
        newUserList.add(user);
        user = createUser(5, Direction.DIRECTION_DOWN, 2);
        newUserList.add(user);

        user = createUser(6, Direction.DIRECTION_DOWN, 5);
        newUserList.add(user);
        user = createUser(6, Direction.DIRECTION_DOWN, 3);
        newUserList.add(user);

        com.setNewUserList(newUserList);
        service.handleWorkLoad(com);
        com = new RequestCom();
        // newUserList = new ArrayList<>();
        //
        // user = new User();
        // user.setId(0);
        // user.setTheGoto(2);
        // newUserList.add(user);
        // com.setGoToList(newUserList);
        service.handleWorkLoad(com);
        moveTime(service, 21);

        // List<User> newUserList = new ArrayList<>();
        // for (int i = 0; i < 30; i++) {
        // User user = new User();
        // user.setCurrentFloor(1);
        // user.setDirection(Direction.DIRECTION_UP);
        // user.setId(i);
        // newUserList.add(user);
        // }
        // com.setNewUserList(newUserList);
        // service.handleWorkLoad(com);
        //
        //
        // com = new RequestCom();
        // newUserList = new ArrayList<>();
        // for (int i = 0; i < 30; i++) {
        // User user = new User();
        // user.setId(i);
        // user.setTheGoto(2);
        // newUserList.add(user);
        // }
        // com.setGoToList(newUserList);
        // service.handleWorkLoad(com);
        // com = new RequestCom();
        // service.handleWorkLoad(com);

    }

    private static int id = 0;

    private static List<User> newUserList = new ArrayList<>();

    private static User createUser(int floor, Direction direction, int theTarget) {
        User user = new User();
        user.setCurrentFloor(floor);
        user.setDirection(direction);
        user.setId(id++);
        user.setTheGoto(theTarget);
        newUserList.add(user);
        return user;
    }

}
