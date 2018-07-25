package com.cha;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cha.model.Lift;
import com.cha.model.RequestCom;
import com.cha.model.User;
import com.cha.service.ChaService;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ElevatorManagerController {

    @Autowired
    private ChaService chaService;

    @GetMapping("/elevator-manager/state")
    public String state() {
        log.info("/state");
        return buildLiftInfo(chaService.state.liftList);
    }

    @GetMapping("/state")
    public String xxstate() {
        return this.state();
    }

    @GetMapping("/reset")
    public String xxreset() {
        return this.reset();
    }

    @PostMapping("/workload")
    public String xxworkload(@RequestBody WorkloadRequest workloadRequest) {
        return this.workload(workloadRequest);
    }

    @GetMapping("/elevator-manager/reset")
    public String reset() {
        log.info("/reset");
        chaService.state.reset();
        return buildLiftInfo(chaService.state.liftList);
    }

    @PostMapping("/elevator-manager/workload")
    public String workload(@RequestBody WorkloadRequest workloadRequest) {
        log.info("/workload");
        RequestCom com = new RequestCom();

        List<User> newUserList = new ArrayList<>();
        if (workloadRequest.requestInfos != null && workloadRequest.requestInfos.length != 0) {
            for (int i = 0; i < workloadRequest.requestInfos.length; i++) {
                int[] requestInfo = workloadRequest.requestInfos[i];
                User newUser = new User();
                newUser.setId(requestInfo[0]);
                newUser.setCurrentFloor(requestInfo[1]);
                newUser.setDirection(Direction.valueOf(requestInfo[2]));
                newUser.setTheGoto(null);
                newUserList.add(newUser);
            }
        }
        com.setNewUserList(newUserList);

        List<User> gotoList = new ArrayList<>();
        if (workloadRequest.gotoInfos != null && workloadRequest.gotoInfos.length != 0) {
            for (int i = 0; i < workloadRequest.gotoInfos.length; i++) {
                int[] gotoInfo = workloadRequest.gotoInfos[i];
                User gotoUser = new User();
                gotoUser.setId(gotoInfo[0]);
                gotoUser.setCurrentFloor(null);
                gotoUser.setDirection(null);
                gotoUser.setTheGoto(gotoInfo[1]);
                gotoList.add(gotoUser);
            }
        }
        com.setGoToList(gotoList);

        chaService.handleWorkLoad(com);

        return buildLiftInfo(chaService.state.liftList);
    }

    private String buildLiftInfo(Lift[] liftMap) {
        StringBuilder liftInfo = new StringBuilder();
        liftInfo.append("{");
        buildLift(liftInfo, liftMap[1], 1);
        liftInfo.append(",");
        buildLift(liftInfo, liftMap[2], 2);
        liftInfo.append(",");
        buildLift(liftInfo, liftMap[3], 3);
        liftInfo.append("}");
        String value = liftInfo.toString();
        log.info("State: {}", value);
        log.info("all state: {}, {},{}", liftMap[1].getHandleCount(), liftMap[2].getHandleCount(),liftMap[3].getHandleCount());
        log.info("statistics: {} === {}", chaService.state.statisitcs, chaService.state.time);
        return value;
    }

    private void buildLift(StringBuilder liftInfo, Lift lift, int index) {
        liftInfo.append("\"").append(index).append("\":{\"floor\":").append(lift.getCurrentFloor())
                .append(",\"users\":[");
        int i = 0;
        for (Integer user : lift.getUserSet().keySet()) {
            if (i != 0) {
                liftInfo.append(",");
            }
            liftInfo.append(user);
            i++;
        }
        liftInfo.append("]}");
    }
}
