package com.staleinit.buddytalk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.staleinit.buddytalk.model.User;

public class MainActivity extends AppCompatActivity {
    private final static String USER = "USER";
    private TextView nameTV;
    private TextView emailTV;
    private TextView genderTV;
    private Button startCall;
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
        nameTV = findViewById(R.id.name_textview);
        emailTV = findViewById(R.id.email_textview);
        genderTV = findViewById(R.id.my_gender_textview);
        startCall = findViewById(R.id.start_call_button);
        startCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallActivity.dialACall(MainActivity.this);
            }
        });
        setUpUserInfo();

    }

    private void setUpUserInfo() {
        if (getIntent() != null && getIntent().getExtras() != null &&
                getIntent().getExtras().containsKey(USER)) {
            mUser = getIntent().getExtras().getParcelable(USER);
        }
        if (mUser != null) {
            nameTV.setText(mUser.username);
            emailTV.setText(mUser.email);
            genderTV.setText(mUser.gender.toString());
        }
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