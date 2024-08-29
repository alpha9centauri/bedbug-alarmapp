package com.varunapp.wakeguard;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmSetter {

    private static final String TAG = "AlarmSetter";

    private final Context context;
    private Database db;

    public AlarmSetter(Context context) {
        this.context = context;
        db = new Database(context);
    }

    public void activateAlarmStart() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmStartAlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent,0);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, db.getTriggerTimeMillis(), pendingIntent);
        Log.d(TAG, "Alarm Set For : " + db.getTriggerTimeMillis());
    }

    public void deactivateAlarmStart() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmStartAlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent,0);

        alarmManager.cancel(pendingIntent);
    }
}


// StartupReceiver AlarmService AlarmStartAlertReceiver mein triggerTime and waitTime and startTimeMillis ab change karna hei