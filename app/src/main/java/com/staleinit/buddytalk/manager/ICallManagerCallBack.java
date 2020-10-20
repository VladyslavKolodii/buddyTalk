package com.staleinit.buddytalk.manager;

import io.agora.rtc.IRtcEngineEventHandler;

public interface ICallManagerCallBack {
    void onRemoteUserLeft(int uid, int reason);

    void onRemoteUserVoiceMuted(int uid, boolean muted);

    void onRemoteUserJoined(int uid, int elapsed);

    void onJoinChannelSuccess();

    void onRejoinChannelSuccess();

    void onLeaveChannel(IRtcEngineEventHandler.RtcStats stats);
}
