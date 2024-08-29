package com.varunapp.wakeguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;


public class AlarmStartAlertReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHelper notificationHelper = new NotificationHelper(context);
        NotificationCompat.Builder nb = notificationHelper.getChannelNotification1();
        notificationHelper.getManager().notify(1, nb.build());
        Database db = new Database(context);

        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra(Database.START_TIME_MILLIS, db.getStartTimeMillis());
        serviceIntent.putExtra(Database.END_TIME_MILLIS, db.getEndTimeMillis());
        serviceIntent.putExtra(Database.TARGET_WEIGHT, db.getTargetWeight());
        serviceIntent.putExtra(Database.EXERCISE_ENABLED, db.isExerciseEnabled());
        ContextCompat.startForegroundService(context, serviceIntent);

        db.add24HrsToTimeMillis();

        AlarmSetter alarm = new AlarmSetter(context);
        alarm.deactivateAlarmStart();
        alarm.activateAlarmStart();

        Intent updateViewBroadcastIntent = new Intent("com.varun.wakeguard.updateViewBroadcastIntent");
        context.sendBroadcast(updateViewBroadcastIntent);
    }
}