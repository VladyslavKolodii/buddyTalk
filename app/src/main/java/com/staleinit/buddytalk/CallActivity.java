package com.staleinit.buddytalk;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
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
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

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
import com.staleinit.buddytalk.api.ApiClient;
import com.staleinit.buddytalk.api.ApiInterface;
import com.staleinit.buddytalk.manager.CallManager;
import com.staleinit.buddytalk.manager.ICallManagerCallBack;
import com.staleinit.buddytalk.model.NotificationBody;
import com.staleinit.buddytalk.model.NotificationModel;
import com.staleinit.buddytalk.model.User;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Locale;
import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class CallActivity extends AppCompatActivity implements ICallManagerCallBack, View.OnClickListener {
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final String TAG = CallActivity.class.getName();
    public static final String BUDDY_USER = "BUDDY_USER";
    private static final String PREFERRED_GENDER = "PREFERRED_GENDER";
    private DatabaseReference mFirebaseDbReference;

    private ImageView searchAnimationView;
    Animatable animationDrawable;
    private Button stopSearchButton;
    private User buddyUser;
    private User mUser;
    private ImageView ivBuddyProfilePic;
    private TextView tvBuddyName;
    private TextView tvConnectionStatus;
    public final static String USER = "USER";
    public final static String CALL_MODE = "CALL_MODE";
    private CallManager callManager;
    private View rootLayout;
    private CallMode mCallMode;
    private View callControlLayout;
    private Gender preferredGender;
    private Button speaker;
    private Button mic;
    private Button endCall;

    @Override
    public void onRemoteUserLeft(int uid, int reason) {
        //showLongToast(String.format(Locale.US, "user %d left %d", (uid & 0xFFFFFFFFL), reason));
        /*View tipMsg = findViewById(R.id.quick_tips_when_use_agora_sdk); // optional UI
        tipMsg.setVisibility(View.VISIBLE);*/
    }

    @Override
    public void onRemoteUserVoiceMuted(int uid, boolean muted) {
        //showLongToast(String.format(Locale.US, "user %d muted or unmuted %b", (uid & 0xFFFFFFFFL), muted));
    }

    @Override
    public void onRemoteUserJoined(int uid, int elapsed) {
        //this is when we need to start the timer.
        showLongToast(String.format(Locale.US, "user %d joined %b", (uid & 0xFFFFFFFFL), elapsed));
    }

    @Override
    public void onJoinChannelSuccess() {
        showLongToast(String.format(Locale.US, "Channel Joined Successfuly!"));
    }

    @Override
    public void onRejoinChannelSuccess() {
        showLongToast(String.format(Locale.US, "Channel Re-Joined Successfuly!"));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.speaker_button:
                v.setSelected(!v.isSelected());
                callManager.onSwitchSpeakerphoneClicked(v.isSelected());
                break;
            case R.id.mic_button:
                v.setSelected(!v.isSelected());
                callManager.onLocalAudioMuteClicked(v.isSelected());
                break;
            case R.id.end_call:
                callManager.leaveChannel();
                finish();
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

    public static void joinACall(MainActivity mainActivity, User mUser, User buddyUser) {
        Intent dialIntent = new Intent(mainActivity, CallActivity.class);
        dialIntent.putExtra(USER, mUser);
        dialIntent.putExtra(BUDDY_USER, buddyUser);
        dialIntent.putExtra(CALL_MODE, CallMode.JOIN);
        mainActivity.startActivity(dialIntent);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        rootLayout = findViewById(R.id.call_activity);
        if (getIntent() != null) {
            mCallMode = (CallMode) getIntent().getExtras().get(CALL_MODE);
            mUser = getIntent().getExtras().getParcelable(USER);
            preferredGender = (Gender) getIntent().getExtras().get(PREFERRED_GENDER);
            if (mCallMode == CallMode.JOIN) {
                buddyUser = getIntent().getExtras().getParcelable(BUDDY_USER);
            }
        }
        try {
            callManager = new CallManager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mFirebaseDbReference = FirebaseDatabase.getInstance().getReference();
        searchAnimationView = findViewById(R.id.searching_rotation_view);
        animationDrawable = (Animatable) searchAnimationView.getDrawable();
        tvBuddyName = findViewById(R.id.peer_name_textview);
        ivBuddyProfilePic = findViewById(R.id.peer_profile_pic);
        tvConnectionStatus = findViewById(R.id.connection_status);
        stopSearchButton = findViewById(R.id.stop_search_button);
        callControlLayout = findViewById(R.id.video_call_utility_buttons);
        speaker = findViewById(R.id.speaker_button);
        speaker.setOnClickListener(this);
        mic = findViewById(R.id.mic_button);
        mic.setOnClickListener(this);
        endCall = findViewById(R.id.end_call);
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
        callControlLayout.setVisibility(View.VISIBLE);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            String channelName = getChannelName();
            if (callManager != null && channelName != null) {
                callManager.initialize(channelName);
            } else {
                someThingWentWrong();
            }
        }

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
                                        return Objects.requireNonNull(input.getValue(User.class))
                                                .gender.equals(preferredGender);
                                    }
                                    return false;
                                }
                            }), DataSnapshot.class);

                    if (availableUsers.length > 0) {
                        int randomIndex = (int) (Math.random() % availableUsers.length);
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

    private Query getQueryByPreferredGender(Gender preferredGender) {
        switch (preferredGender) {
            /*case MALE:
                return mFirebaseDbReference.child("users").equalTo("gender", "MALE")
                        .equalTo("isAvailable", "true");
            case FEMALE:
                return mFirebaseDbReference.child("users").equalTo("gender", "FEMALE")
                        .equalTo("isAvailable", "true");
            case BOTH:*/
            default:
                return mFirebaseDbReference.child("users").orderByChild("isAvailable").equalTo(true);
        }
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
            String channelName = getChannelName();
            if (callManager != null && channelName != null) {
                sendNotificationToBuddy(buddyUser);
                callManager.initialize(channelName);
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
    }

    @Override
    protected void onDestroy() {
        setUserAvailability(true);
        callManager.leaveChannel();
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
                    String channelName = getChannelName();
                    if (callManager != null && channelName != null) {
                        callManager.initialize(channelName);
                    } else {
                        someThingWentWrong();
                    }
                } else {
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    finish();
                }
                break;
            }
        }
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


    public final void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

}
