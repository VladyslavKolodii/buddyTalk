package com.staleinit.buddytalk.model;

public class DataModel {

    private String userName;
    private String userId;

    public DataModel(String name, String userId) {
        this.userName = name;
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}

