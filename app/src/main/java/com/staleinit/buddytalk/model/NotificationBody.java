package com.staleinit.buddytalk.model;

import com.google.gson.annotations.SerializedName;

public class NotificationBody {
    @SerializedName("to")
    private String topic;

    /*@SerializedName("notification")
    private NotificationModel notification;*/

    @SerializedName("data")
    private User data;

    public NotificationBody(String topic, NotificationModel notification, User data) {
        this.topic = topic;
        //this.notification = notification;
        this.data = data;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

   /* public NotificationModel getNotification() {
        return notification;
    }

    public void setNotification(NotificationModel notification) {
        this.notification = notification;
    }*/

    public User getData() {
        return data;
    }

    public void setData(User data) {
        this.data = data;
    }
}
