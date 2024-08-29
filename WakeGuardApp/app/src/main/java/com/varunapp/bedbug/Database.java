package com.varunapp.wakeguard;

import static android.content.Context.MODE_PRIVATE;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

public class Database {

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String ALARM_ENABLED = "alarmEnabledBoolean";
    public static final String START_TIME_MILLIS = "startTimeMillis";
    public static final String END_TIME_MILLIS = "endTimeMillis";
    public static final String WAIT_TIME_SECS = "waitTime";
    public static final String TARGET_WEIGHT = "targetWeight";
    public static final String EXERCISE_ENABLED = "exerciseEnabled";
    public static final String RECALIBRATION_FACTOR = "recalibrationFactor";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final boolean DEFAULT_ALARM_ENABLED = false;
    private static final long DEFAULT_START_TIME_MILLIS = Calendar.getInstance().getTimeInMillis() + 1 * 60 * 1000;
    private static final long DEFAULT_END_TIME_MILLIS = Calendar.getInstance().getTimeInMillis() + 3 * 60 * 1000;
    private static final int DEFAULT_WAIT_TIME_SECS = 30 * 60;
    private static final int DEFAULT_TARGET_WEIGHT = 10;
    public static final boolean DEFAULT_EXERCISE_ENABLED = true;
    public static final int DEFAULT_RECALIBRATION_FACTOR = 23500;
    public static final long HRS24_IN_MILLISECONDS = 24L * 60 * 60 * 1000;

    public Database(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        if(!isAlarmEnabled()) {
            refreshTimeMillis();
        }
    }

    public void setAlarmEnabled(boolean alarmEnabled) {
        if(!alarmEnabled)
            refreshTimeMillis();
        editor.putBoolean(ALARM_ENABLED, alarmEnabled);
        editor.apply();
    }

    public boolean isAlarmEnabled() {
        return sharedPreferences.getBoolean(ALARM_ENABLED, DEFAULT_ALARM_ENABLED);
    }

    public void setStartTimeMillis(long startTimeMillis) {
        if(isAlarmEnabled())
            return;
        refreshTimeMillis();
        editor.putLong(START_TIME_MILLIS, startTimeMillis);
        editor.apply();
        if(startTimeMillis >= getEndTimeMillis())
            add24HrsToEndTimeMillis();
        refreshTimeMillis();
    }

    public long getStartTimeMillis() {
        return sharedPreferences.getLong(START_TIME_MILLIS, DEFAULT_START_TIME_MILLIS);
    }

    public void setEndTimeMillis(long endTimeMillis) {
        if(isAlarmEnabled())
            return;
        refreshTimeMillis();
        editor.putLong(END_TIME_MILLIS, endTimeMillis);
        editor.apply();
        if(getStartTimeMillis() >= endTimeMillis)
            add24HrsToEndTimeMillis();
        refreshTimeMillis();
    }

    public long getEndTimeMillis() {
        return sharedPreferences.getLong(END_TIME_MILLIS, DEFAULT_END_TIME_MILLIS);
    }

    public void setWaitTime(int waitTime) {
        editor.putInt(WAIT_TIME_SECS, waitTime);
        editor.apply();
    }

    public int getWaitTimeSecs() {
        return sharedPreferences.getInt(WAIT_TIME_SECS, DEFAULT_WAIT_TIME_SECS);
    }
    
    public void setTargetWeight(int targetWeight) {
        editor.putInt(TARGET_WEIGHT, targetWeight);
        editor.apply();
    }

    public int getTargetWeight() {
        return sharedPreferences.getInt(TARGET_WEIGHT, DEFAULT_TARGET_WEIGHT);
    }

    public void setExerciseEnabled(boolean value) {
        editor.putBoolean(EXERCISE_ENABLED, value);
        editor.apply();
    }

    public boolean isExerciseEnabled() {
        return sharedPreferences.getBoolean(EXERCISE_ENABLED, DEFAULT_EXERCISE_ENABLED);
    }

    public void setRecalibrationFactor(int recalibrationFactor) {
        editor.putInt(RECALIBRATION_FACTOR, recalibrationFactor);
    }

    public int getRecalibrationFactor() {
        return sharedPreferences.getInt(RECALIBRATION_FACTOR, DEFAULT_RECALIBRATION_FACTOR);
    }

    public long getTriggerTimeMillis() {
        return (getStartTimeMillis() - getWaitTimeSecs() * 1000L);
    }

    public void add24HrsToTimeMillis() {
        add24HrsToStartTimeMillis();
        add24HrsToEndTimeMillis();
    }

    private long convertTimeMillisToTodaysTime(long timeMillis) {
        Calendar currTime = Calendar.getInstance();
        int dayOfMonth = currTime.get(Calendar.DAY_OF_MONTH);
        int month = currTime.get(Calendar.MONTH);
        int year = currTime.get(Calendar.YEAR);
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timeMillis);
        time.set(year, month, dayOfMonth);
        timeMillis = time.getTimeInMillis();

        return timeMillis;
    }

    private void refreshTimeMillis() {
        long startTimeMillis = sharedPreferences.getLong(START_TIME_MILLIS, DEFAULT_START_TIME_MILLIS);
        long endTimeMillis = sharedPreferences.getLong(END_TIME_MILLIS, DEFAULT_END_TIME_MILLIS);
        startTimeMillis = convertTimeMillisToTodaysTime(startTimeMillis);
        endTimeMillis = convertTimeMillisToTodaysTime(endTimeMillis);
        if(startTimeMillis >= endTimeMillis)
            endTimeMillis = endTimeMillis + HRS24_IN_MILLISECONDS;
        editor.putLong(START_TIME_MILLIS, startTimeMillis);
        editor.putLong(END_TIME_MILLIS, endTimeMillis);
        editor.apply();
    }

    private void add24HrsToEndTimeMillis() {
        long endTimeMillis = getEndTimeMillis();
        endTimeMillis = endTimeMillis + HRS24_IN_MILLISECONDS;
        editor.putLong(END_TIME_MILLIS, endTimeMillis);
        editor.apply();
    }

    private void add24HrsToStartTimeMillis() {
        long startTimeMillis = getStartTimeMillis();
        startTimeMillis = startTimeMillis + HRS24_IN_MILLISECONDS;
        editor.putLong(START_TIME_MILLIS, startTimeMillis);
        editor.apply();
    }
}
