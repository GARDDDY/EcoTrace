package com.gy.ecotrace;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class PushNotificationsService extends FirebaseMessagingService {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        String title = message.getNotification().getTitle();
        String text = message.getNotification().getBody();
        Log.d("Notification new",title.toString()+" "+text.toString());
        final String CHANNEL_ID = "HEADS_UP_NOTIFICATION";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Heads Up Notification", NotificationManager.IMPORTANCE_DEFAULT);
        Log.d("Notification new 1",title.toString()+" "+text.toString());
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Log.d("Notification new 2",title.toString()+" "+text.toString());
        Notification.Builder notification = new Notification.Builder(this, CHANNEL_ID).setContentTitle(title).setContentText(text).setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(true);
        Log.d("Notification new 3",title.toString()+" "+text.toString());
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            return;
        }
        Log.d("Notification new 4",title.toString()+" "+text.toString());
        NotificationManagerCompat.from(this).notify(1, notification.build());
        Log.d("Notification new 5",title.toString()+" "+text.toString());
        super.onMessageReceived(message);
    }
}
