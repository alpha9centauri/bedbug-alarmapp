package com.varunapp.wakeguard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener, ActivateAlarmConfirmDialogBox.ActivateAlarmConfirmDialogListener {

    private static final String TAG = "MainActivity";

    private TextView startTimeText;
    private TextView endTimeText;
    private TextView alarmActiveText;
    private TextView bedStatusText;
    private TextView calibrationFactorStatusText;
    private TextView recalibrationFactorText;
    private SwitchCompat switchEnableDisable;
    private Button recalibrateBtn;
    private Button setStartTimeBtn;
    private Button setEndTimeBtn;

    private boolean startTimeSet;
    private boolean endTimeSet;
    private boolean playBedStatusThread;
    private boolean playMonitorStatusThread;
    private double currWeight;
    private String calibrationFactorStatus;
    private String monitorStats;

    AlarmSetter alarm;
    Database db;

    MqttConnection bedStatusConnection;
    MqttConnection recalibrateConnection;
    MqttConnection calibrationFactorConnection;
    MqttConnection monitorStatusConnection;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateViews();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "OnCreate");

        startTimeText = findViewById(R.id.startTimeText);
        endTimeText = findViewById(R.id.endTimeText);
        setStartTimeBtn = findViewById(R.id.setStartTimeBtn);
        setEndTimeBtn = findViewById(R.id.setEndTimeBtn);
        alarmActiveText = findViewById(R.id.activationStatus);
        switchEnableDisable = findViewById(R.id.switchEnableDisable);
        bedStatusText = findViewById(R.id.bedStatus);
        recalibrateBtn = findViewById(R.id.recalibrateButton);
        recalibrationFactorText = findViewById(R.id.recalibrationFactorText);
        calibrationFactorStatusText = findViewById(R.id.calibrationFactorStatus);
        startTimeSet = true;
        endTimeSet = true;

        IntentFilter filter = new IntentFilter("com.varun.wakeguard.updateViewBroadcastIntent");
        registerReceiver(broadcastReceiver, filter);

        alarm = new AlarmSetter(this);
        db = new Database(this);

        bedStatusConnection = new MqttConnection(this, MqttConnection.BROKER_URL, MqttConnection.CLIENT_ID, MqttConnection.TOPIC_KGVALUE);
        bedStatusConnection.connectAndSubscribe();
        calibrationFactorConnection = new MqttConnection(this, MqttConnection.BROKER_URL, MqttConnection.CLIENT_ID, MqttConnection.TOPIC_CALIBRATION_FACTOR);
        calibrationFactorConnection.connectAndSubscribe();
        monitorStatusConnection = new MqttConnection(this, MqttConnection.BROKER_URL, MqttConnection.CLIENT_ID, MqttConnection.TOPIC_MONITOR);
        monitorStatusConnection.connectAndSubscribe();
        recalibrateConnection = new MqttConnection(this, MqttConnection.BROKER_URL, MqttConnection.CLIENT_ID, MqttConnection.TOPIC_TARE);
        recalibrateConnection.connectAndSubscribe();

        playBedStatusThread = true;
        BedStatusRunnable bedStatusRunnable = new BedStatusRunnable();
        new Thread(bedStatusRunnable).start();

        playMonitorStatusThread = true;
        MonitorStatusRunnable monitorStatusRunnable = new MonitorStatusRunnable();
        new Thread(monitorStatusRunnable).start();

        updateViews();

        setStartTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(db.isAlarmEnabled()){
                    Toast.makeText(MainActivity.this,
                            "Can not edit when alarm is enabled. Disable the alarm first.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                startTimeSet = false;
                DialogFragment timePicker = new TimePickerFragment();
                timePicker.show(getSupportFragmentManager(), "time picker");
            }
        });

        setEndTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(db.isAlarmEnabled()){
                    Toast.makeText(MainActivity.this,
                            "Can not edit when alarm is enabled. Disable the alarm first.",
                            Toast.LENGTH_SHORT).show();

                    return;
                }
                endTimeSet = false;
                DialogFragment timePicker = new TimePickerFragment();
                timePicker.show(getSupportFragmentManager(), "time picker");
            }
        });

        switchEnableDisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(switchEnableDisable.isChecked())
                    enableAlarm();
                else
                    disableAlarm();

            }
        });

        recalibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.setRecalibrationFactor(Integer.parseInt(recalibrationFactorText.getText().toString()));
                if (bedStatusConnection.isReceivingMessages()) {
                    recalibrateConnection.publish(String.valueOf(db.getRecalibrationFactor()));
                }
                else {
                    Toast.makeText(MainActivity.this, "Recalibration Failed. No Connection.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        playBedStatusThread = true;
        BedStatusRunnable bedStatusRunnable = new BedStatusRunnable();
        new Thread(bedStatusRunnable).start();

        playMonitorStatusThread = true;
        MonitorStatusRunnable monitorStatusRunnable = new MonitorStatusRunnable();
        new Thread(monitorStatusRunnable).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playMonitorStatusThread = false;
        playBedStatusThread = false;
    }

    public void confirmToStartServiceNow(long startTimeMillis, long endTimeMillis, int targetWeight, boolean exerciseEnabled) {
        ActivateAlarmConfirmDialogBox activateAlarmConfirmDialogBox = new ActivateAlarmConfirmDialogBox(startTimeMillis, endTimeMillis, targetWeight, exerciseEnabled);
        activateAlarmConfirmDialogBox.show(getSupportFragmentManager(), "activate alarm confirm dialog box");
    }

    @Override
    public void startServiceNow(long startTimeMillis, long endTimeMillis, int targetWeight, boolean exerciseEnabled) {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        serviceIntent.putExtra(Database.START_TIME_MILLIS, startTimeMillis);
        serviceIntent.putExtra(Database.END_TIME_MILLIS, endTimeMillis);
        serviceIntent.putExtra(Database.TARGET_WEIGHT, targetWeight);
        serviceIntent.putExtra(Database.EXERCISE_ENABLED, exerciseEnabled);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);

        if(!startTimeSet && endTimeSet) {
            startTimeSet = true;
            db.setStartTimeMillis(c.getTimeInMillis());
        }
        else if(startTimeSet && !endTimeSet) {
            endTimeSet = true;
            db.setEndTimeMillis(c.getTimeInMillis());
        }
        updateViews();
        Log.d(TAG, "Time Set :- StartTimeMillis= " + db.getStartTimeMillis() + " and EndTimeMillis= " + db.getEndTimeMillis());
    }

    public void enableAlarm() {
        db.setAlarmEnabled(true);
        long currTimeMillis = Calendar.getInstance().getTimeInMillis();

        Log.d(TAG, "CurrTime : " + currTimeMillis + " TriggerTime : " + db.getTriggerTimeMillis() + " Start Time : " + db.getStartTimeMillis());
        if(currTimeMillis < db.getTriggerTimeMillis()){
            alarm.activateAlarmStart();
        }
        else {
            if(currTimeMillis < db.getEndTimeMillis())
                confirmToStartServiceNow(db.getStartTimeMillis(), db.getEndTimeMillis(), db.getTargetWeight(), db.isExerciseEnabled());

            db.add24HrsToTimeMillis();
            alarm.deactivateAlarmStart();
            alarm.activateAlarmStart();
        }
        updateViews();
    }

    public void disableAlarm() {
        db.setAlarmEnabled(false);
        alarm.deactivateAlarmStart();
        updateViews();
    }

    public void updateViews() {
        switchEnableDisable.setChecked(db.isAlarmEnabled());
        recalibrationFactorText.setText(String.valueOf(db.getRecalibrationFactor()));

        Calendar st = Calendar.getInstance();
        st.setTimeInMillis(db.getStartTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd LLLL yyyy HH:mm aaa EEE");
        String dateTime = simpleDateFormat.format(st.getTime());
        startTimeText.setText(dateTime);

        Calendar et = Calendar.getInstance();
        et.setTimeInMillis(db.getEndTimeMillis());
        simpleDateFormat = new SimpleDateFormat("dd LLLL yyyy HH:mm aaa EEE");
        dateTime = simpleDateFormat.format(et.getTime());
        endTimeText.setText(dateTime);

        Log.d(TAG, "updateViews() Called - MainActivity view updated");
    }

    class BedStatusRunnable implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, "BedStatusRunnable Starting");
            while(playBedStatusThread) {

                Handler threadHandler = new Handler(Looper.getMainLooper());
                threadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        bedStatusText.setText("No Connection ... ");
                        calibrationFactorStatusText.setText("No Connection ... ");
                    }
                });

                while(playBedStatusThread && !bedStatusConnection.isReceivingMessages() ) {
                    try {
                        Thread.sleep(1000);
                        Log.d(TAG, "Waiting for messages ... ");

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                String msg;
                String fac;
                while(playBedStatusThread && bedStatusConnection.isReceivingMessages() ) {
                    fac = calibrationFactorConnection.getMessage();
                    msg = bedStatusConnection.getMessage();

                    currWeight = Double.parseDouble(msg);
                    calibrationFactorStatus = fac;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    threadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            bedStatusText.setText("Reading = " + currWeight);
                            calibrationFactorStatusText.setText("Calibration Factor = " + calibrationFactorStatus);
                        }
                    });

                }

            }
            Log.d(TAG, "BedStatusRunnable Stopping");
        }
    }

    class MonitorStatusRunnable implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, "MonitorStatusRunnable Starting");
            while(playMonitorStatusThread) {

                Handler threadHandler = new Handler(Looper.getMainLooper());
                threadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        alarmActiveText.setText("No Connection ... ");
                    }
                });

                while(playMonitorStatusThread && !monitorStatusConnection.isReceivingMessages() ) {
                    try {
                        Thread.sleep(1000);
                        Log.d(TAG, "Waiting for messages ... ");

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                String stat;
                while(playMonitorStatusThread && monitorStatusConnection.isReceivingMessages() ) {
                    stat = monitorStatusConnection.getMessage();

                    monitorStats = stat;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    threadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            alarmActiveText.setText(monitorStats);
                            if(monitorStats.charAt(0) == 'T')
                                alarmActiveText.setTextColor(Color.GREEN);
                            else if(monitorStats.equals("Alarm Inactive"))
                                alarmActiveText.setTextColor(Color.GRAY);
                            else if(monitorStats.equals("Disabled") || monitorStats.equals("Enable Failure"))
                                alarmActiveText.setTextColor(Color.RED);
                            else
                                alarmActiveText.setTextColor(Color.YELLOW);
                        }
                    });

                }

            }
            Log.d(TAG, "MonitorStatusRunnable Stopping");
        }
    }



    public boolean internetIsConnected() {
        try {
            String command = "ping -c 1 google.com";
            return (Runtime.getRuntime().exec(command).waitFor() == 0);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "OnDestroy");
        playBedStatusThread = false;
        playMonitorStatusThread = false;
        unregisterReceiver(broadcastReceiver);
    }
}