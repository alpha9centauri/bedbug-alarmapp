package com.varunapp.wakeguard;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import androidx.core.app.NotificationCompat;


public class NotificationHelper extends ContextWrapper {
    public static final String channelID1= "Alarm Notification";
    public static final String channelName1 = "Alarm Notification";

    public static final String channelID3 = "Service Notification";
    public static final String channelName3 = "Service Notification";

    public static final String channelID5 = "Boot Notification";
    public static final String channelName5 = "Boot Notification";

    private NotificationManager mManager;

    public NotificationHelper(Context base) {
        super(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannels() {
        NotificationChannel channel1 = new NotificationChannel(channelID1, channelName1, NotificationManager.IMPORTANCE_HIGH);
        NotificationChannel channel3 = new NotificationChannel(channelID3, channelName3, NotificationManager.IMPORTANCE_HIGH);
        NotificationChannel channel5 = new NotificationChannel(channelID5, channelName5, NotificationManager.IMPORTANCE_HIGH);
        getManager().createNotificationChannel(channel1);
        getManager().createNotificationChannel(channel3);
        getManager().createNotificationChannel(channel5);
    }

    public NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        return mManager;
    }

    public NotificationCompat.Builder getChannelNotification1() {
        return new NotificationCompat.Builder(getApplicationContext(), channelID1)
                .setContentTitle("Bedbug")
                .setContentText("Wakeup !!")
                .setSmallIcon(R.drawable.ic_android);
    }

    public NotificationCompat.Builder getChannelNotification3() {
        return new NotificationCompat.Builder(getApplicationContext(), channelID3)
                .setContentTitle("Bedbug")
                .setContentText("Calling monitor ... ")
                .setSmallIcon(R.drawable.ic_android);
    }


    public NotificationCompat.Builder getChannelNotification5() {
        return new NotificationCompat.Builder(getApplicationContext(), channelID5)
                .setContentTitle("Bedbug")
                .setContentText("Reinitializing Bedbug")
                .setSmallIcon(R.drawable.ic_android);
    }
}