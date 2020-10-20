package com.staleinit.buddytalk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.staleinit.buddytalk.constants.Gender;
import com.staleinit.buddytalk.manager.UserManager;
import com.staleinit.buddytalk.model.User;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getName();
    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth mAuth;
    private CallbackManager mCallbackManager;
    private LoginButton mLoginButton;
    private SignInButton mGoogleSignIn;
    private DatabaseReference mDatabase;
    private GoogleSignInClient mGoogleSignInClient;
    private LinearLayout progressLayout;
    private Spinner gender;
    private UserManager userManager;
    private TextView progressMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mLoginButton = findViewById(R.id.fb_login_button);
        mGoogleSignIn = findViewById(R.id.google_sign_in_button);
        progressLayout = findViewById(R.id.progress_layout);
        progressMessage = findViewById(R.id.progress_message);
        userManager = UserManager.getInstance((BuddyTalkApplication) getApplication());
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mCallbackManager = CallbackManager.Factory.create();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
        handleFacebookLogin();
        handleGoogleLogin();
        handleGender();
    }

    private void handleGender() {
        gender = findViewById(R.id.mygenderspinner);
    }

    private void handleGoogleLogin() {
        mGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressLayout(getString(R.string.google_signin_in_progress));
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    private void showProgressLayout(String message) {
        progressLayout.setVisibility(View.VISIBLE);
        progressMessage.setText(message);
        gender.setEnabled(false);
        mLoginButton.setEnabled(false);
        mGoogleSignIn.setEnabled(false);
    }

    private void hideProgressLayout() {
        progressLayout.setVisibility(View.GONE);
        gender.setEnabled(true);
        mLoginButton.setEnabled(true);
        mGoogleSignIn.setEnabled(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(userManager.getLoggedInUser());
        }
    }

    private void handleFacebookLogin() {
        mLoginButton.setReadPermissions("email", "public_profile");
        mLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                hideProgressLayout();

            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                hideProgressLayout();

            }
        });

    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        showProgressLayout(getString(R.string.msg_setting_up_the_account));
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            handleSignInSuccess();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

    private void handleSignInSuccess() {
        FirebaseUser user = mAuth.getCurrentUser();
        hideProgressLayout();
        if (user != null) {

            User user1 = new User(user.getUid(), user.getDisplayName(),
                    user.getEmail(), getGender(), true, user.getPhotoUrl() != null ?
                    user.getPhotoUrl().toString() : "");
            mDatabase.child("users").child(user1.userId).setValue(user1);
            userManager.saveUser(user1);
            updateUI(user1);
        } else {
            //User object is null
        }
    }

    private Gender getGender() {
        switch (gender.getSelectedItemPosition()) {
            case 1:
                return Gender.FEMALE;
            case 0:
            default:
                return Gender.MALE;
        }
    }

    private void updateUI(User user) {
        hideProgressLayout();
        if (user != null) {
            MainActivity.launch(this, user);
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                hideProgressLayout();
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        showProgressLayout(getString(R.string.msg_setting_up_the_account));
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            handleSignInSuccess();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            //Snackbar.make(mBinding.mainLayout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

}
