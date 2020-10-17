package com.staleinit.buddytalk.manager;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.staleinit.buddytalk.BuddyTalkApplication;
import com.staleinit.buddytalk.model.User;

public class UserManager {
    private static UserManager userManager;
    private SharedPreferences sharedPreferences;
    private static final String PREFERENCE_NAME = "user_preference";
    private static final String USER = "user";

    private UserManager(BuddyTalkApplication buddyTalkApplication) {
        sharedPreferences = buddyTalkApplication
                .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public synchronized static UserManager getInstance(BuddyTalkApplication buddyTalkApplication) {
        if (userManager == null) {
            userManager = new UserManager(buddyTalkApplication);
        }
        return userManager;
    }

    @Nullable
    public User getLoggedInUser() {
        String userNameString = sharedPreferences.getString(USER, null);
        User user = null;
        if (userNameString != null) {
            user = new Gson().fromJson(userNameString, User.class);
        }
        return user;
    }

    public void saveUser(User user) {
        sharedPreferences.edit().putString(USER, new Gson().toJson(user)).apply();
    }
}
