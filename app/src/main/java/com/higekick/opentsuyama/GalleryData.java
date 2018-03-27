package com.higekick.opentsuyama;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
}
