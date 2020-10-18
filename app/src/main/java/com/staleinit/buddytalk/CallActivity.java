package com.staleinit.buddytalk;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.Iterables;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.staleinit.buddytalk.api.ApiClient;
import com.staleinit.buddytalk.api.ApiInterface;
import com.staleinit.buddytalk.manager.CallManager;
import com.staleinit.buddytalk.manager.ICallManagerCallBack;
import com.staleinit.buddytalk.model.DataModel;
import com.staleinit.buddytalk.model.NotificationBody;
import com.staleinit.buddytalk.model.NotificationModel;
import com.staleinit.buddytalk.model.User;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class CallActivity extends AppCompatActivity implements ICallManagerCallBack {
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final String TAG = CallActivity.class.getName();
    public static final String BUDDY_USER = "BUDDY_USER";
    private DatabaseReference mFirebaseDbReference;
    private ImageView searchAnimationView;
    AnimatedVectorDrawable animationDrawable;
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
    }

    public enum CallMode {
        DIAL, JOIN
    }

    public static void dialACall(MainActivity mainActivity, User mUser) {
        Intent dialIntent = new Intent(mainActivity, CallActivity.class);
        dialIntent.putExtra(USER, mUser);
        dialIntent.putExtra(CALL_MODE, CallMode.DIAL);
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
        animationDrawable = (AnimatedVectorDrawable) searchAnimationView.getDrawable();
        tvBuddyName = findViewById(R.id.peer_name_textview);
        ivBuddyProfilePic = findViewById(R.id.peer_profile_pic);
        tvConnectionStatus = findViewById(R.id.connection_status);
        animationDrawable.start();
        stopSearchButton = findViewById(R.id.stop_search_button);
        stopSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animationDrawable.stop();
                finish();
            }
        });
        if (mCallMode == CallMode.DIAL) {
            searchBuddy();
        }
    }

    private void searchBuddy() {
        Query query = mFirebaseDbReference.child("users").orderByChild("isAvailable").equalTo(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataSnapshot[] availableUsers = Iterables.
                            toArray(snapshot.getChildren(), DataSnapshot.class);
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
        else
            return String.format("%s:%s", mUser.userId, buddyUser.userId);
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
