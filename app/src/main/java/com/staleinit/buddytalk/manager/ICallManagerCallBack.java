package com.staleinit.buddytalk.manager;

public interface ICallManagerCallBack {
    void onRemoteUserLeft(int uid, int reason);

    void onRemoteUserVoiceMuted(int uid, boolean muted);

    void onRemoteUserJoined(int uid, int elapsed);

    void onJoinChannelSuccess();

    void onRejoinChannelSuccess();
}
