package com.higekick.opentsuyama;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.preference.DialogPreference;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;

import com.higekick.opentsuyama.util.Const;
import com.higekick.opentsuyama.util.DialogFragmentActivity;
import com.higekick.opentsuyama.util.MyDialogFragment;
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
            final String prfx;
            if (getKey().equals(res.getString(R.string.title_json_download))) {
                prfx = Const.JSON_PRFX;
            } else if (getKey().equals(res.getString(R.string.title_image_download))) {
                prfx = Const.IMG_PRFX;
            } else {
                prfx = "";
            }

            switch (Util.netWorkCheck(getContext())) {
                case NONE:{
                    return;
                }
                case WIFI:{
                    showDialog();
                    Util.startService(this.getContext(), prfx);
                    break;
                }
                case OTHER:
                    DialogFragmentActivity.startActivity((Activity) getContext(), prfx);
                    break;
            }
        }
    }

    private void showDialog(){
        ProgressDialogCustome dialog = new ProgressDialogCustome(getContext());
        dialog.setup();
        dialog.show();
    }
}
