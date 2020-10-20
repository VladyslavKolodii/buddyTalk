package com.staleinit.buddytalk.manager;

import android.content.Context;
import android.util.Log;

import com.staleinit.buddytalk.R;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;

public class CallManager {
    private static final String TAG = CallManager.class.getName();
    private Context context;
    private ICallManagerCallBack iCallManagerCallBack;
    private String channelName;
    private RtcEngine mRtcEngine; // Tutorial Step 1
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() { // Tutorial Step 1

        /**
         * Occurs when a remote user (Communication)/host (Live Broadcast) leaves the channel.
         *
         * There are two reasons for users to become offline:
         *
         *     Leave the channel: When the user/host leaves the channel, the user/host sends a goodbye message. When this message is received, the SDK determines that the user/host leaves the channel.
         *     Drop offline: When no data packet of the user or host is received for a certain period of time (20 seconds for the communication profile, and more for the live broadcast profile), the SDK assumes that the user/host drops offline. A poor network connection may lead to false detections, so we recommend using the Agora RTM SDK for reliable offline detection.
         *
         * @param uid ID of the user or host who
         * leaves
         * the channel or goes offline.
         * @param reason Reason why the user goes offline:
         *
         *     USER_OFFLINE_QUIT(0): The user left the current channel.
         *     USER_OFFLINE_DROPPED(1): The SDK timed out and the user dropped offline because no data packet was received within a certain period of time. If a user quits the call and the message is not passed to the SDK (due to an unreliable channel), the SDK assumes the user dropped offline.
         *     USER_OFFLINE_BECOME_AUDIENCE(2): (Live broadcast only.) The client role switched from the host to the audience.
         */
        @Override
        public void onUserOffline(final int uid, final int reason) { // Tutorial Step 4
            Log.d(TAG, "User offline :" + reason);
            iCallManagerCallBack.onRemoteUserLeft(uid, reason);
        }

        /**
         * Occurs when a remote user stops/resumes sending the audio stream.
         * The SDK triggers this callback when the remote user stops or resumes sending the audio stream by calling the muteLocalAudioStream method.
         *
         * @param uid ID of the remote user.
         * @param muted Whether the remote user's audio stream is muted/unmuted:
         *
         *     true: Muted.
         *     false: Unmuted.
         */
        @Override
        public void onUserMuteAudio(final int uid, final boolean muted) { // Tutorial Step 6
            Log.d(TAG, "UserMute Audio :" + muted);
            iCallManagerCallBack.onRemoteUserVoiceMuted(uid, muted);
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            Log.d(TAG, "User joined the channel :" + uid);
            iCallManagerCallBack.onRemoteUserJoined(uid, elapsed);
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            Log.d(TAG, "Successfully joined the channel :" + channel);
            iCallManagerCallBack.onJoinChannelSuccess();
        }

        @Override
        public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onRejoinChannelSuccess(channel, uid, elapsed);
            Log.d(TAG, "Successfully re-joined the channel :" + channel);
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            //Log.d(TAG, "Successfully left the channel :" + stats.users);
            iCallManagerCallBack.onLeaveChannel(stats);
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            Log.d(TAG, "Error:" + err);
        }
    };

    public CallManager(Context context) throws Exception {
        this.context = context;
        if (context instanceof ICallManagerCallBack) {
            iCallManagerCallBack = (ICallManagerCallBack) context;
        } else {
            throw new Exception("Caller must implement ICallManagerCallBack");
        }
    }

    public void initialize(String channelName) {
        this.channelName = channelName;
        initializeAgoraEngine();     // Tutorial Step 1
        joinChannel();               // Tutorial Step 2
    }

    // Tutorial Step 1
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(context, context.getString(R.string.agora_app_id), mRtcEventHandler);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            mRtcEngine.enableAudio();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    // Tutorial Step 2
    private void joinChannel() {
        // Sets the channel profile of the Agora RtcEngine.
        // CHANNEL_PROFILE_COMMUNICATION(0): (Default) The Communication profile. Use this profile in one-on-one calls or group calls, where all users can talk freely.
        // CHANNEL_PROFILE_LIVE_BROADCASTING(1): The Live-Broadcast profile. Users in a live-broadcast channel have a role as either broadcaster or audience. A broadcaster can both send and receive streams; an audience can only receive streams.
        // Allows a user to join a channel.
        mRtcEngine.joinChannel(null, channelName, "Extra Goes Here", 0); // if you do not specify the uid, we will generate the uid for you
    }

    // Tutorial Step 3
    public void leaveChannel() {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }
    }


    // Tutorial Step 5
    public void onSwitchSpeakerphoneClicked(boolean isSelected) {
        // Enables/Disables the audio playback route to the speakerphone.
        //
        // This method sets whether the audio is routed to the speakerphone or earpiece. After calling this method, the SDK returns the onAudioRouteChanged callback to indicate the changes.
        mRtcEngine.setEnableSpeakerphone(isSelected);
    }

    // Tutorial Step 7
    public void onLocalAudioMuteClicked(Boolean isMuted) {
        // Stops/Resumes sending the local audio stream.
        mRtcEngine.muteLocalAudioStream(isMuted);
    }
}

