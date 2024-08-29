package com.varunapp.wakeguard;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class ActivateAlarmConfirmDialogBox extends AppCompatDialogFragment {

    private ActivateAlarmConfirmDialogListener listener;
    long startTimeMillis;
    long endTimeMillis;
    int targetWeight;
    boolean exerciseEnabled;

    public ActivateAlarmConfirmDialogBox(long startTimeMillis, long endTimeMillis, int targetWeight, boolean exerciseEnabled) {
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.targetWeight = targetWeight;
        this.exerciseEnabled = exerciseEnabled;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Do you want to start now ?").setCancelable(false)
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.startServiceNow(startTimeMillis, endTimeMillis, targetWeight, exerciseEnabled);
                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (ActivateAlarmConfirmDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    "must implement ExampleDialogListener");
        }
    }

    public interface ActivateAlarmConfirmDialogListener {
        void startServiceNow(long startTimeMillis, long endTimeMillis, int targetWeight, boolean exerciseEnabled);
    }
}
