package com.staleinit.buddytalk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.staleinit.buddytalk.manager.UserManager;
import com.staleinit.buddytalk.model.User;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.staleinit.buddytalk.CallActivity.BUDDY_USER;
import static com.staleinit.buddytalk.CallActivity.CALL_MODE;
import static com.staleinit.buddytalk.CallActivity.USER;

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
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            createNotification(remoteMessage.getNotification(), buddyUser);
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private void createNotification(RemoteMessage.Notification notification, User buddyUser) {
        if (buddyUser != null) {
            RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.expanded_notification);
            Intent acceptCallIntent = new Intent(this, CallActivity.class);
            acceptCallIntent.putExtra(USER, UserManager.getInstance((BuddyTalkApplication) getApplicationContext()).getLoggedInUser());
            acceptCallIntent.putExtra(BUDDY_USER, buddyUser);
            acceptCallIntent.putExtra(CALL_MODE, CallActivity.CallMode.JOIN);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 100,
                    acceptCallIntent, FLAG_UPDATE_CURRENT);
            notificationLayout.setOnClickPendingIntent(R.id.right_action_button, pendingIntent);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                    this, getString(R.string.channel_name))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCustomBigContentView(notificationLayout)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(true);


            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(buddyUser.hashCode(), notificationBuilder.build());
        }

    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
    }
}
