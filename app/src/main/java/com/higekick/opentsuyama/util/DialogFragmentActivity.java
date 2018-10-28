package com.higekick.opentsuyama.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.higekick.opentsuyama.R;

public class DialogFragmentActivity extends AppCompatActivity {
    private static String mPrfx;

    public static void startActivity(Activity activity, String prfx){
        mPrfx = prfx;
        Intent intent = new Intent(activity, DialogFragmentActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyDialogFragment df = new MyDialogFragment();
        Resources res = getResources();
        df.setTitle(res.getString(R.string.title_download))
                .setMessage(res.getString(R.string.message_recommend_wifi))
                .setOnPositiveClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showDialog();
                        Util.startService(getApplicationContext(), mPrfx);
                    }
                })
                .setOnNegativeClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        df.show(getSupportFragmentManager(), "dialog");
    }

    private void showDialog(){
        ProgressDialogCustome dialog = new ProgressDialogCustome(this);
        dialog.setup();
        dialog.setOnDownloadFinishListener(new ProgressDialogCustome.OnDownloadFinishListener() {
            @Override
            public void onDownloadFinish() {
                finish();
            }
        });
        dialog.show();
    }
}
