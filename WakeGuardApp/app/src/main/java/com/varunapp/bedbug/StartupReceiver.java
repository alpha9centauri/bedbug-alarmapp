package com.varunapp.wakeguard;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class StartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationHelper notificationHelper = new NotificationHelper(context);
            NotificationCompat.Builder nb = notificationHelper.getChannelNotification5();
            notificationHelper.getManager().notify(5, nb.build());

            Database db = new Database(context);
            AlarmSetter alarm = new AlarmSetter(context);

            Calendar c = Calendar.getInstance();
            long currTimeMillis = c.getTimeInMillis();

            if(db.isAlarmEnabled()) {

                if(currTimeMillis < db.getTriggerTimeMillis()) {// phone switched off and back on before the start time
                    alarm.activateAlarmStart();
                }
                else {// phone switched on after the start time
                    if(currTimeMillis < db.getEndTimeMillis()) {
                        Intent serviceIntent = new Intent(context, AlarmService.class);
                        serviceIntent.putExtra(Database.START_TIME_MILLIS, db.getStartTimeMillis());
                        serviceIntent.putExtra(Database.END_TIME_MILLIS, db.getEndTimeMillis());
                        serviceIntent.putExtra(Database.TARGET_WEIGHT, db.getTargetWeight());
                        serviceIntent.putExtra(Database.EXERCISE_ENABLED, db.isExerciseEnabled());
                        ContextCompat.startForegroundService(context, serviceIntent);
                    }

                    db.add24HrsToTimeMillis();

                    alarm = new AlarmSetter(context);
                    alarm.deactivateAlarmStart();
                    alarm.activateAlarmStart();
                }
            }
        }
    }
}
