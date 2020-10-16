package com.staleinit.buddytalk;

import android.app.Application;

import com.facebook.FacebookSdk;

public class BuddyTalkApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FacebookSdk.sdkInitialize(this);
    }
}
