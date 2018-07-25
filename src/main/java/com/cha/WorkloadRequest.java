package com.cha;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkloadRequest {

    @JsonProperty("request")
    public int[][] requestInfos;

    @JsonProperty("goto")
    public int[][] gotoInfos;
}
