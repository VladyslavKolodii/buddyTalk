package com.staleinit.buddytalk.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.IgnoreExtraProperties;
import com.staleinit.buddytalk.Gender;

@IgnoreExtraProperties
public class User implements Parcelable {
    public String userId;
    public String username;
    public String email;
    public Gender gender;
    public boolean isAvailable;

    public User() {

    }

    public User(String userId, String username, String email, Gender gender, boolean isAvailable) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.gender = gender;
        this.isAvailable = isAvailable;
    }

    protected User(Parcel in) {
        userId = in.readString();
        username = in.readString();
        email = in.readString();
        gender = Gender.values()[in.readInt()];
        isAvailable = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(username);
        dest.writeString(email);
        dest.writeInt(gender.ordinal());
        dest.writeByte((byte) (isAvailable ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
