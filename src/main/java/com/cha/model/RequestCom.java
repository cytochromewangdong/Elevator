package com.cha.model;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RequestCom {
    private List<User> newUserList;
    private List<User> goToList;
}
