package com.staleinit.buddytalk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.staleinit.buddytalk.manager.UserManager;
import com.staleinit.buddytalk.model.User;

public class MainActivity extends AppCompatActivity {
    private final static String USER = "USER";
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvGender;
    private Button startCall;
    private ImageView ivUserProfilePic;
    private User mUser;
    private FirebaseAuth mAuth;

    public static void launch(Context context, User user) {
        Intent mainPageIntent = new Intent(context, MainActivity.class);
        mainPageIntent.putExtra(USER, user);
        context.startActivity(mainPageIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        tvName = findViewById(R.id.name_textview);
        tvEmail = findViewById(R.id.email_textview);
        tvGender = findViewById(R.id.my_gender_textview);
        startCall = findViewById(R.id.start_call_button);
        ivUserProfilePic = findViewById(R.id.profile_pic);
        setUpUserInfo();
        startCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUserAvailability(false);
                CallActivity.dialACall(MainActivity.this, mUser);
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        setUserAvailability(true);
    }

    private void setUserAvailability(boolean isAvailable) {
        if (mUser != null) {
            FirebaseDatabase.getInstance().getReference().child("users").child(mUser.userId)
                    .child("isAvailable").setValue(isAvailable);
        }
    }

    private void setUpUserInfo() {
        if (getIntent() != null && getIntent().getExtras() != null &&
                getIntent().getExtras().containsKey(USER)) {
            mUser = getIntent().getExtras().getParcelable(USER);
        }
        if (mUser != null) {
            tvName.setText(mUser.username);
            tvEmail.setText(mUser.email);
            tvGender.setText(mUser.gender.toString());
            Glide.with(this).load(mUser.profilePic).into(ivUserProfilePic);
            setUserAvailability(true);
            subscribeToATopic();
        }
    }

    private void subscribeToATopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(mUser.userId)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MainActivity.this, "subscribed", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                logoutUser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        UserManager.getInstance((BuddyTalkApplication) getApplication()).logoutUser();
        //fb logout
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isFBLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isFBLoggedIn) {
            LoginManager.getInstance().logOut();
        }
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}