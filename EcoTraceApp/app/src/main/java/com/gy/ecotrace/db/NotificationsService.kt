package com.gy.ecotrace.db

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gy.ecotrace.Globals
import com.gy.ecotrace.R
import com.gy.ecotrace.ui.more.profile.ProfileActivity

class NotificationsService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("MSG", "received smth")

        val notification = message.data
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val title = notification["title"]
        val text = notification["body"]
        val action = notification["clickAction"]
        val color = notification["color"]

        val channelId = "HEADS_UP_NOTIFICATION"
        val channelName = "Heads Up Notification"

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel description"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)


        val pendingIntent = when (action) {
            "OPEN_PROFILE" -> {
                message.data["friendSender"]?.let {
                    val intent = Intent(this, ProfileActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    Globals.getInstance().setString("CurrentlyWatching", it)
                    PendingIntent.getActivity(
                        this, 0, intent,
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }

            else -> null
        }


        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(Color.parseColor(color))
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)


        NotificationManagerCompat.from(this).notify(1, notificationBuilder.build())
    }

}