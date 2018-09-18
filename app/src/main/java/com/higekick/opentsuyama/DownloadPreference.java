package com.higekick.opentsuyama;

import android.content.Context;
import android.content.res.Resources;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

import com.higekick.opentsuyama.util.Const;
import com.higekick.opentsuyama.util.ProgressDialogCustome;
import com.higekick.opentsuyama.util.Util;

public class DownloadPreference extends DialogPreference {
    public DownloadPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DownloadPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateDialogView() {
        return super.onCreateDialogView();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            Resources res = this.getContext().getResources();
            if (getKey().equals(res.getString(R.string.title_json_download))) {
                if (!Util.netWorkCheck(getContext())) {
                    return;
                }
                showDialog();
                Util.startService(this.getContext(), Const.JSON_PRFX);
            } else if (getKey().equals(res.getString(R.string.title_image_download))) {
                if (!Util.netWorkCheck(getContext())) {
                    return;
                }
                showDialog();
                Util.startService(this.getContext(), Const.IMG_PRFX);
            }
        }
    }

    private void showDialog(){
        ProgressDialogCustome dialog = new ProgressDialogCustome(getContext());
        dialog.setup();
        dialog.show();
    }
}
