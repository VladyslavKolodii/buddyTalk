package com.staleinit.buddytalk.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UserCallLog {
    public String userId;
    public long callDuration;

    public UserCallLog() {

    }

    public UserCallLog(String userId, long callDuration) {
        this.userId = userId;
        this.callDuration = callDuration;
    }
}
