package com.varunapp.wakeguard;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class AlarmService extends Service {

    private static final String TAG = "AlarmService";

    private boolean monitorEnabled;
    private long startTimeMillis, endTimeMillis;
    private int targetWeight;
    private boolean exerciseEnabled;

    private PowerManager.WakeLock wakeLock;
    private MqttConnection enableConnection, monitorStatusConnection;
    private WifiManager.WifiLock wfl;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeGuard:WakeLock");
        wakeLock.acquire();

        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wfl = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "WifiLock");
        if (!wfl.isHeld()) {
            wfl.acquire();
        }

        NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());
        NotificationCompat.Builder nb = notificationHelper.getChannelNotification3();

        startForeground(3, nb.build());
    }


    @Override
    public int  onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OnStartCommand");

        startTimeMillis = intent.getLongExtra(Database.START_TIME_MILLIS, 0);
        endTimeMillis = intent.getLongExtra(Database.END_TIME_MILLIS, 0);
        exerciseEnabled = intent.getBooleanExtra(Database.EXERCISE_ENABLED, false);
        targetWeight = intent.getIntExtra(Database.TARGET_WEIGHT, 10);

        monitorEnabled = false;

        // monitor status connection
        monitorStatusConnection = new MqttConnection(getApplicationContext(), MqttConnection.BROKER_URL, MqttConnection.CLIENT_ID, MqttConnection.TOPIC_MONITOR);
        monitorStatusConnection.connectAndSubscribe();

        // enable connection
        enableConnection = new MqttConnection(getApplicationContext(), MqttConnection.BROKER_URL, MqttConnection.CLIENT_ID, MqttConnection.TOPIC_ENABLE);
        enableConnection.connectAndSubscribe();

        InnerServiceThread thread = new InnerServiceThread();
        thread.start();

        return START_NOT_STICKY;
    }

    class InnerServiceThread extends Thread {
        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void run() {
            Log.d(TAG, "Inside Thread");
            super.run();
            int enableFailureCounter = 0;

            Long t = Calendar.getInstance().getTimeInMillis();
            Log.d(TAG, "StartTimeMillis : " + startTimeMillis);
            Log.d(TAG, "CurrTimeMillis  : " + t);
            Log.d(TAG, "EndTimeMillis   : " + endTimeMillis);
            if(t < endTimeMillis) {
                Log.d(TAG, "Curr Time Less Than End Time");
            } else {
                Log.d(TAG, "Curr Time More Than End Time");
            }
            while(!monitorEnabled && Calendar.getInstance().getTimeInMillis() < endTimeMillis) {
                Log.d(TAG, "Inside Service Loop");
                Log.d(TAG, "Checkpoint 1 ===================================================================================================");

//                if(!monitorEnabled && !isConnected()) {
//                    if(!mediaPlayer.isPlaying()) {
//                        mediaPlayer.start();
//                    }
//                }
                while(!monitorEnabled && Calendar.getInstance().getTimeInMillis() < endTimeMillis && !isConnected()) {
                    Log.d(TAG, "Switch on your wifi or mobile data ... ");
                    Handler threadHandler = new Handler(Looper.getMainLooper());
                    threadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Switch on your wifi or mobile data ... ", Toast.LENGTH_SHORT).show();
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
//                if(mediaPlayer.isPlaying()) {
//                    mediaPlayer.stop();
//                }

                Log.d(TAG, "Checkpoint 2 ===================================================================================================");
                while (!monitorEnabled && Calendar.getInstance().getTimeInMillis() < endTimeMillis && isConnected() && !monitorStatusConnection.isReceivingMessages()) {
                    try {
                        Thread.sleep(1000);
                        Log.d(TAG, "Waiting for Connection ... ");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "Checkpoint 3 ===================================================================================================");
                if (!monitorEnabled && Calendar.getInstance().getTimeInMillis() < endTimeMillis && isConnected() && monitorStatusConnection.isReceivingMessages()) {
                    String mesg = new String("");
                    long currTimeMillis = Calendar.getInstance().getTimeInMillis();
                    int waitTimeSecsUpdated = 0;
                    int monitoringTimeSecsUpdated = 0;

                    // Building Message to send
                    // exerciseEnabled MonitoringTime WaitTime TargetWeight
                    // 1 300 20 10-> exercise is enabled, monitoring time is 300 secs, wait time is 20 secs, targetWeight is 10 kgs
                    // 0 300 0 40-> exercise is disabled, monitoring time is 300 secs, wait time is 0 secs, targetWeight is 40 kgs

                    // Exercise Enabled
                    if(exerciseEnabled)
                        mesg += "1 ";
                    else
                        mesg += "0 ";

                    // Monitoring Time and WaitingTime
                    if(currTimeMillis < startTimeMillis) {
                        waitTimeSecsUpdated = (int) ((startTimeMillis - currTimeMillis) / 1000);
                        monitoringTimeSecsUpdated = (int) ((endTimeMillis - startTimeMillis) / 1000);
                    }
                    else {
                        waitTimeSecsUpdated = 0;
                        monitoringTimeSecsUpdated = (int) ((endTimeMillis - currTimeMillis) / 1000);
                    }
                    mesg += String.valueOf(monitoringTimeSecsUpdated) + " ";
                    mesg += String.valueOf(waitTimeSecsUpdated) + " ";

                    // Target Weight
                    mesg += String.valueOf(targetWeight);

                    Log.d(TAG, "Checkpoint 4 Enable Checkpoint ===================================================================================================");
                    Log.d(TAG, "Publishing the Message : " + mesg);
                    enableConnection.publish(mesg);

                    // Waiting for it to receive
                    while(!monitorEnabled && Calendar.getInstance().getTimeInMillis() < endTimeMillis && isConnected() && monitorStatusConnection.isReceivingMessages() && !enableConnection.isDeliveryStatus()){
                        try {
                            Thread.sleep(1000);
                            Log.d(TAG, "Waiting for delivery ... ");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // Waiting for it to get enabled
                    int retryCounter = 0;
                    int waitForDisabled = 0;
                    while(!monitorEnabled && Calendar.getInstance().getTimeInMillis() < endTimeMillis && isConnected() && monitorStatusConnection.isReceivingMessages() && enableConnection.isDeliveryStatus()) {
                        try {
                            Log.d(TAG, "Waiting for monitor to get enabled ... ");
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        String mesgReceived = monitorStatusConnection.getMessage();

                        if(mesgReceived.equals("not disabled yet")){
                            Log.d(TAG, "not disabled yet");
                            waitForDisabled++;
                            if(waitForDisabled > 5)
                                break;
                        }
                        else if(mesgReceived.equals("Enable Failure")){
                            Log.d(TAG, "Enable Failure");
                            enableFailureCounter++;
                            break;
                        }
                        else if(mesgReceived.equals("Alarm Inactive") || mesgReceived.equals("Online Disabled")){
                            Log.d(TAG, "Alarm Inactive");
                            retryCounter++;
                            if(retryCounter > 10)
                                break;
                        }
                        else {
                            Log.d(TAG, "Enabled");
                            monitorEnabled = true;
                            break;
                        }
                    }

                    if(enableFailureCounter > 5) {
                        Log.d(TAG, "Service Failed due to failure at monitor module");
                        break;
                    }
                }
                enableConnection.setDeliveryStatus(false);
            }
            stopForeground(true);
            stopSelf();
            Log.d(TAG, "Out of Thread Checkpoint ================================================================================================");
        }
    }


    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo;
        networkInfo = connectivityManager.getActiveNetworkInfo();

        if(networkInfo == null) {
            return false;
        }
        return true;
    }

    public boolean internetIsConnected() {
        try {
            String command = "ping -c 1 google.com";
            return (Runtime.getRuntime().exec(command).waitFor() == 0);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isMobileConnected() {
        if(isConnected()) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo;
            networkInfo = connectivityManager.getActiveNetworkInfo();

            return networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        }
        else
            return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy  $$$$$$$$$$$$$$$$$$$$$$$$$$$$ Service Destroyed $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

        if (wfl != null) {
            if (wfl.isHeld()) {
                wfl.release();
            }
        }
        wakeLock.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
