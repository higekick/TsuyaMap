package com.higekick.opentsuyama.util;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.higekick.opentsuyama.R;

public class ProgressDialogCustome extends ProgressDialog {
    public ProgressDialogCustome(Context context) {
        super(context);
    }

    MyBroadcastReceiver mReciever;
    OnDownloadFinishListener onDownloadFinishListener;
    public void setOnDownloadFinishListener(OnDownloadFinishListener l) {
        onDownloadFinishListener = l;
    }

    public interface OnDownloadFinishListener {
        void onDownloadFinish();
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent i) {
            Bundle b = i.getExtras();
            if (b.containsKey("progress")) {
                int progress = i.getExtras().getInt("progress",0);
                setProgress(progress);
            } else if (b.containsKey("max")) {
                int max = i.getExtras().getInt("max",0);
                setMax(max);
            } else if (b.containsKey("finish")) {
                if (onDownloadFinishListener != null) {
                    onDownloadFinishListener.onDownloadFinish();
                }
                dismiss();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();
        mReciever = new MyBroadcastReceiver();
        getContext().registerReceiver(mReciever, new IntentFilter(Const.ACTION_RETRIEVE_PROGRESS));
    }

    public void setup(){
        setTitle(R.string.title_now_download);
        setMessage(getContext().getResources().getString(R.string.message_pleasewait));
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL); // プログレスダイアログのスタイルをバースタイルに設定
        // プログレスダイアログのキャンセルが可能かどうかを設定（バックボタンでダイアログをキャンセルできないようにする）
        setCancelable(false);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mReciever != null) {
            getContext().unregisterReceiver(mReciever);
            mReciever = null;
        }
    }
}
