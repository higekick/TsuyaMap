package com.higekick.opentsuyama.util;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.higekick.opentsuyama.R;

public class MyDialogFragment extends DialogFragment {
    String title;
    String message;
    String positiveButton;
    String negativeButton;
    DialogInterface.OnClickListener onPositiveClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
    };
    DialogInterface.OnClickListener onNegativeClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
    };

    public MyDialogFragment setMessage(String message) {
        this.message = message;
        return this;
    }

    public MyDialogFragment setTitle(String title) {
        this.title = title;
        return this;
    }

    public MyDialogFragment setOnPositiveClickListener(DialogInterface.OnClickListener onPositiveClickListener) {
        this.onPositiveClickListener = onPositiveClickListener;
        return this;
    }

    public MyDialogFragment setOnNegativeClickListener(DialogInterface.OnClickListener onNegativeClickListener) {
        this.onNegativeClickListener = onNegativeClickListener;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        positiveButton = getContext().getResources().getString(R.string.ok);
        negativeButton = getContext().getResources().getString(R.string.cancel);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(message)
                .setPositiveButton(positiveButton, onPositiveClickListener)
                .setNegativeButton(negativeButton, onNegativeClickListener);
        return builder.create();
    }
}
