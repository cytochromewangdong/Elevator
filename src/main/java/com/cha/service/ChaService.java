package com.cha.service;

import org.springframework.stereotype.Service;

import com.cha.model.GlobalState;
import com.cha.model.RequestCom;

@Service
public class ChaService {

    public GlobalState state = new GlobalState();

    // make it thread safe
    public synchronized void handleWorkLoad(RequestCom com) {
        // Reset the statues for all the lists
        state.beforeTimebeat();
        // Update the lifts with the goto
        state.updateLiftWithGoto(com.getGoToList());
        // let the new come users to be onsite.
        state.updateLevelWithNewUsers(com.getNewUserList());
        // For the lift with people, let it satisfy the guests.
        state.satisfyGuests();
        // 为已有用户的电梯，载入当层用户
        state.loadLiftContainingGuests();
        // Let us make a plan for the load
        state.doPlan();
        // Let us load the guests 
//        state.load();
//        state.redoIt();
        state.postTimeBeat();
    }
}
