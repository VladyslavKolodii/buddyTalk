package com.staleinit.buddytalk.activity;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.staleinit.buddytalk.BuddyTalkApplication;
import com.staleinit.buddytalk.R;
import com.staleinit.buddytalk.utils.CountUpTimer;
import com.staleinit.buddytalk.api.ApiClient;
import com.staleinit.buddytalk.api.ApiInterface;
import com.staleinit.buddytalk.constants.Gender;
import com.staleinit.buddytalk.manager.UserManager;
import com.staleinit.buddytalk.model.NotificationBody;
import com.staleinit.buddytalk.model.NotificationModel;
import com.staleinit.buddytalk.model.User;
import com.staleinit.buddytalk.model.UserCallLog;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import okhttp3.ResponseBody;
import retrofit2.Callback;

public class CallActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final String TAG = CallActivity.class.getName();
    public static final String BUDDY_USER = "BUDDY_USER";
    private static final String PREFERRED_GENDER = "PREFERRED_GENDER";
    private DatabaseReference mFirebaseDbReference;
    private long totalCallDuration = 0;
    private ImageView searchAnimationView;
    Animatable animationDrawable;
    private Button stopSearchButton;
    private User buddyUser;
    private User mUser;
    private ImageView ivBuddyProfilePic;
    private TextView tvBuddyName;
    private TextView tvConnectionStatus;
    private TextView tvCallMinutes;
    public final static String USER = "USER";
    public final static String CALL_MODE = "CALL_MODE";
    private String channelName;
    private View rootLayout;
    private CallMode mCallMode;
    private View callControlLayout;
    private Gender preferredGender;
    CountUpTimer timer;
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
            //end the call here and update call log
            showLongToast(buddyUser.username + " has disconnected");
            endTheCall();
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
            if (muted) {
                showLongToast(buddyUser.username + " has muted");
            }
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            Log.d(TAG, "User joined the channel :" + uid);
            //this is when we need to start the timer.
            //showLongToast(buddyUser.username + " has joined");
            if (mCallMode == CallMode.DIAL) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showCallDurationTicker();
                    }
                });
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            Log.d(TAG, "Successfully joined the channel :" + channel);
            if (mCallMode == CallMode.JOIN) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showCallDurationTicker();
                    }
                });
            }
        }

        @Override
        public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onRejoinChannelSuccess(channel, uid, elapsed);
            Log.d(TAG, "Successfully re-joined the channel :" + channel);
            if (mCallMode == CallMode.JOIN) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showCallDurationTicker();
                    }
                });
            }
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            //Log.d(TAG, "Successfully left the channel :" + stats.users);
            if (mCallMode == CallMode.DIAL) {
                totalCallDuration = stats.totalDuration;
                final UserCallLog callLog = new UserCallLog();
                final User loggedInUser = UserManager.getInstance((BuddyTalkApplication) getApplication())
                        .getLoggedInUser();
                if (loggedInUser != null) {
                    callLog.userId = loggedInUser.userId;
                    Query query = mFirebaseDbReference
                            .child("CallLog")
                            .orderByChild("userId")
                            .equalTo(loggedInUser.userId);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                DataSnapshot[] callLogs = Iterables.toArray(
                                        snapshot.getChildren(), DataSnapshot.class);
                                if (callLogs.length > 0) {
                                    UserCallLog existingCallLog = callLogs[0].getValue(UserCallLog.class);
                                    if (existingCallLog != null) {
                                        totalCallDuration += existingCallLog.callDuration;
                                    }
                                }
                            }
                            callLog.callDuration = totalCallDuration;
                            mFirebaseDbReference.child("CallLog").child(loggedInUser.userId).setValue(callLog);
                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            finish();
                        }
                    });
                }
            } else {
                finish();
            }
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            Log.d(TAG, "Error:" + err);
        }
    };


    private void endTheCall() {
        leaveChannel();
        if (timer != null) {
            timer.cancel();
        }
        callControlLayout.setVisibility(View.GONE);
        stopSearchButton.setVisibility(View.GONE);
    }


    private void showCallDurationTicker() {
        tvConnectionStatus.setText(R.string.msg_in_call_with_buddy);
        stopSearchButton.setVisibility(View.GONE);
        callControlLayout.setVisibility(View.VISIBLE);
        tvCallMinutes.setVisibility(View.VISIBLE);
        startCounter();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.speaker_button:
                v.setSelected(!v.isSelected());
                onSwitchSpeakerphoneClicked(v.isSelected());
                break;
            case R.id.mic_button:
                v.setSelected(!v.isSelected());
                onLocalAudioMuteClicked(v.isSelected());
                break;
            case R.id.end_call:
                endTheCall();
                break;
        }
    }

    public enum CallMode {
        DIAL, JOIN
    }

    public static void dialACall(MainActivity mainActivity, User mUser, Gender preferredGender) {
        Intent dialIntent = new Intent(mainActivity, CallActivity.class);
        dialIntent.putExtra(USER, mUser);
        dialIntent.putExtra(CALL_MODE, CallMode.DIAL);
        dialIntent.putExtra(PREFERRED_GENDER, preferredGender);
        mainActivity.startActivity(dialIntent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        rootLayout = findViewById(R.id.call_activity);
        if (getIntent() != null && getIntent().getExtras() != null) {
            mCallMode = (CallMode) getIntent().getExtras().get(CALL_MODE);
            mUser = getIntent().getExtras().getParcelable(USER);
            preferredGender = (Gender) getIntent().getExtras().get(PREFERRED_GENDER);
            if (mCallMode == CallMode.JOIN) {
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
                buddyUser = getIntent().getExtras().getParcelable(BUDDY_USER);
            }
        }

        mFirebaseDbReference = FirebaseDatabase.getInstance().getReference();
        searchAnimationView = findViewById(R.id.searching_rotation_view);
        animationDrawable = (Animatable) searchAnimationView.getDrawable();
        tvBuddyName = findViewById(R.id.peer_name_textview);
        ivBuddyProfilePic = findViewById(R.id.peer_profile_pic);
        tvConnectionStatus = findViewById(R.id.connection_status);
        stopSearchButton = findViewById(R.id.stop_search_button);
        callControlLayout = findViewById(R.id.video_call_utility_buttons);
        tvCallMinutes = findViewById(R.id.call_minutes_textview);
        Button speaker = findViewById(R.id.speaker_button);
        speaker.setOnClickListener(this);
        Button mic = findViewById(R.id.mic_button);
        mic.setOnClickListener(this);
        Button endCall = findViewById(R.id.end_call);
        endCall.setOnClickListener(this);

        stopSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animationDrawable.stop();
                finish();
            }
        });

        if (mCallMode == CallMode.DIAL) {
            animationDrawable.start();
            searchBuddy();
        } else {
            joinCallWithBuddy();
        }
    }

    private void joinCallWithBuddy() {
        tvConnectionStatus.setText(R.string.msg_in_call_with_buddy);
        tvBuddyName.setText(buddyUser.username);
        Glide.with(this).load(buddyUser.profilePic).into(ivBuddyProfilePic);
        stopSearchButton.setVisibility(View.GONE);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            joinAgoraChannelCall();
        }

    }

    private void joinAgoraChannelCall() {
        channelName = getChannelName();
        if (channelName != null) {
            initialize();
        } else {
            someThingWentWrong();
        }
    }

    public void initialize() {
        initializeAgoraEngine();     // Tutorial Step 1
        joinChannel();               // Tutorial Step 2
    }

    private void startCounter() {
        timer = new CountUpTimer(30000) {
            public void onTick(int second) {
                int hours = second / 3600;
                int minutes = (second - hours * 3600) / 60;
                int seconds = (second - hours * 3600) - minutes * 60;
                tvCallMinutes.setText(String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));
            }
        };
        timer.start();
    }

    private void searchBuddy() {
        Query query = mFirebaseDbReference.child("users").orderByChild("isAvailable").equalTo(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataSnapshot[] availableUsers = Iterables.toArray(
                            Iterables.filter(snapshot.getChildren(), new Predicate<DataSnapshot>() {
                                @Override
                                public boolean apply(@NullableDecl DataSnapshot input) {
                                    if (input != null && input.getValue(User.class) != null) {
                                        if (preferredGender == Gender.BOTH) {
                                            return true;
                                        } else {
                                            return Objects.requireNonNull(input.getValue(User.class))
                                                    .gender.equals(preferredGender);
                                        }
                                    }
                                    return false;
                                }
                            }), DataSnapshot.class);

                    if (availableUsers.length > 0) {
                        int randomIndex = new Random().nextInt(availableUsers.length);
                        buddyUser = availableUsers[randomIndex].getValue(User.class);
                        if (buddyUser != null) {
                            connectCallWithBuddy(buddyUser);
                        } else {
                            noBuddyFound();
                        }
                    } else {
                        noBuddyFound();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noBuddyFound();
            }
        });

    }

    private void noBuddyFound() {
        tvBuddyName.setText(R.string.no_buddy_found);
        animationDrawable.stop();
    }

    private void connectCallWithBuddy(User buddyUser) {
        setUserAvailability(false);
        tvConnectionStatus.setText(R.string.calling_buddy);
        tvBuddyName.setText(buddyUser.username);
        Glide.with(this).load(buddyUser.profilePic).into(ivBuddyProfilePic);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            channelName = getChannelName();
            if (channelName != null) {
                sendNotificationToBuddy(buddyUser);
                initialize();
            } else {
                someThingWentWrong();
            }
        }
    }

    private void sendNotificationToBuddy(User buddyUser) {
        NotificationBody rootModel = new NotificationBody("/topics/" + buddyUser.userId,
                new NotificationModel("Come on in", "Join the call"),
                mUser);

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        retrofit2.Call<ResponseBody> responseBodyCall = apiService.sendNotification(rootModel);

        responseBodyCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                Log.d(TAG, "Notification sent Successfully");
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {

            }
        });

    }

    private void someThingWentWrong() {
        Snackbar.make(rootLayout, R.string.something_went_wrong, Snackbar.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        setUserAvailability(true);
        mRtcEngine = null;
        super.onDestroy();
    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i(TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    joinAgoraChannelCall();
                } else {
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    finish();
                }
                break;
            }
        }
    }

    // Tutorial Step 1
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(this, getString(R.string.agora_app_id), mRtcEventHandler);
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

    private String getChannelName() {
        if (mUser == null || buddyUser == null)
            return null;

        if (mCallMode == CallMode.DIAL) {
            return String.format("%s:%s", mUser.userId, buddyUser.userId);
        } else {
            return String.format("%s:%s", buddyUser.userId, mUser.userId);
        }
    }

    private void setUserAvailability(boolean isAvailable) {
        if (buddyUser != null) {
            FirebaseDatabase.getInstance().getReference().child("users").child(buddyUser.userId)
                    .child("isAvailable").setValue(isAvailable);
        }
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


    public final void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

}
