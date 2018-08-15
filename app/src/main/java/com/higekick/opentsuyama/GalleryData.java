package com.higekick.opentsuyama;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.higekick.opentsuyama.util.Const;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 2017/08/21.
 */

public class GalleryData extends AbstractContentData {

    public List<String> picUrls;

    @Override
    public void importFromJson(JSONObject j){
        try {
            super.importFromJson(j);
            this.picUrls = new ArrayList<>();
            JSONArray picUrlArray = j.getJSONArray("pic_url");
            for (int i=0; i < picUrlArray.length(); i++){
                this.picUrls.add(picUrlArray.getString(i));
            }
        } catch (JSONException ex){
            Log.d("GalleryData","unhandled Exception.", ex);
        }
    }

    @Override
    public AbstractContentData newInstance() {
        return new GalleryData();
    }

    @Override
    public String getFragmentClassName(){
        return MainGalleryFragment.class.getSimpleName();
    }

    @Override
    public void importFromFile(Context con, String dir) {
        super.importFromFile(con, dir);
        String pathDir = con.getFilesDir().getAbsolutePath() + "/" + Const.IMG_PRFX + "/" + dir;
        File fileDir = new File(pathDir);
        String[] files = fileDir.list();
        this.picUrls = new ArrayList<>();
        for(String file : files) {
            if (!file.equals("dirname.txt")) {
                picUrls.add(pathDir + "/" + file);
            }
        }
    }
}
