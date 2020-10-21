package com.staleinit.buddytalk.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.staleinit.buddytalk.BuddyTalkApplication;
import com.staleinit.buddytalk.R;
import com.staleinit.buddytalk.activity.CallActivity;
import com.staleinit.buddytalk.activity.MainActivity;
import com.staleinit.buddytalk.constants.Gender;
import com.staleinit.buddytalk.manager.UserManager;
import com.staleinit.buddytalk.model.User;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static com.staleinit.buddytalk.activity.CallActivity.BUDDY_USER;
import static com.staleinit.buddytalk.activity.CallActivity.CALL_MODE;
import static com.staleinit.buddytalk.activity.CallActivity.USER;
import static com.staleinit.buddytalk.activity.MainActivity.CANCEL_CALL;

public class NotificationService extends FirebaseMessagingService {
    private static final String TAG = NotificationService.class.getName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // ...

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        User buddyUser = null;
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            buddyUser = new User();
            buddyUser.username = remoteMessage.getData().get("username");
            buddyUser.profilePic = remoteMessage.getData().get("profilePic");
            buddyUser.userId = remoteMessage.getData().get("userId");
            buddyUser.gender = Gender.valueOf(remoteMessage.getData().get("gender"));
            buddyUser.isAvailable = Boolean.parseBoolean(remoteMessage.getData().get("isAvailable"));
            buddyUser.email = remoteMessage.getData().get("email");
        }

        // Check if message contains a notification payload.
        if (buddyUser != null) {
            //Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            createNotification(buddyUser);
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private void createNotification(User buddyUser) {

        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.expanded_notification);
        Intent acceptCallIntent = new Intent(this, CallActivity.class);
        acceptCallIntent.putExtra(USER, UserManager.getInstance((BuddyTalkApplication) getApplicationContext()).getLoggedInUser());
        acceptCallIntent.putExtra(BUDDY_USER, buddyUser);
        acceptCallIntent.putExtra(CALL_MODE, CallActivity.CallMode.JOIN);

        Intent cancelIntent = new Intent(this, MainActivity.class);
        cancelIntent.putExtra(CANCEL_CALL, true);

        PendingIntent pendingAccpectIntent = PendingIntent.getActivity(this, 100,
                acceptCallIntent, FLAG_CANCEL_CURRENT);
        PendingIntent pendingCancelIntent = PendingIntent.getActivity(this, 200,
                cancelIntent, FLAG_CANCEL_CURRENT);

        notificationLayout.setOnClickPendingIntent(R.id.right_action_button, pendingAccpectIntent);
        notificationLayout.setOnClickPendingIntent(R.id.left_action_button, pendingCancelIntent);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this, getString(R.string.channel_name))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setCustomBigContentView(notificationLayout)
                .setCustomHeadsUpContentView(notificationLayout)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true);

        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_INSISTENT;


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(1, notification);


    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
    }
}
