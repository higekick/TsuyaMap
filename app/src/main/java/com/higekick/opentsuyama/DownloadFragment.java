package com.higekick.opentsuyama;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.higekick.opentsuyama.util.ProgressDialogCustome;

public class DownloadFragment extends DialogFragment {
    private static ProgressDialogCustome progressDialog = null;
    private ProgressDialogCustome.OnDownloadFinishListener onDownloadFinishListener;

    public void setOnDownloadFinishListener(ProgressDialogCustome.OnDownloadFinishListener l) {
        onDownloadFinishListener = l;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (progressDialog == null)
            progressDialog = new ProgressDialogCustome(getContext());

        progressDialog.setOnDownloadFinishListener(onDownloadFinishListener);
        progressDialog.setup();
        return progressDialog;
    }

    // progressDialog取得
    @Override
    public Dialog getDialog(){
        return progressDialog;
    }

    // ProgressDialog破棄
    @Override
    public void onDestroy(){
        super.onDestroy();

        progressDialog = null;
    }
}
