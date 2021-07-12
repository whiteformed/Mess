package com.example.mess;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import static com.example.mess.Mess.CH_MESSAGE;
import static com.example.mess.Mess.KEY_PHOTO_ICON;
import static com.example.mess.Mess.MESSAGE;
import static com.example.mess.Mess.SENDER;
import static com.example.mess.Mess.UID;
import static com.example.mess.Mess.USERNAME;

public class MessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        showNotification(remoteMessage);
    }

    private void showNotification(RemoteMessage remoteMessage) {
        Map<String, String> remoteMap = remoteMessage.getData();
        String senderUid = remoteMap.get(SENDER);
        String senderUsername = remoteMap.get(USERNAME);
        String message = remoteMap.get(MESSAGE);
        assert senderUid != null;

        int notificationId = senderUid.hashCode();

        Mess application = (Mess) getApplication();
        if (application != null) {
            Activity currentActivity = application.getCurrentActivity();
            if (currentActivity instanceof MainActivity) return;
            else if (currentActivity instanceof ChatActivity) {
                String targetUid = ((ChatActivity) currentActivity).getTargetUid();
                if (targetUid.equals(senderUid)) return;
            }
        }

        Notification.Builder builder = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ?
                new Notification.Builder(this, CH_MESSAGE) : new Notification.Builder(this);

        Intent resultIntent = new Intent(this, ChatActivity.class);

        resultIntent.putExtra(UID, senderUid);
        resultIntent.putExtra(USERNAME, senderUsername);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT);

        if (message != null && message.equals(KEY_PHOTO_ICON)) message = getString(R.string.photo);

        Notification notification = builder
                .setContentTitle(senderUsername)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notif_message)
                .setContentIntent(resultPendingIntent)
                .setGroup(senderUid)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, notification);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Mess application = (Mess) getApplication();
        application.saveToken(token);
    }
}