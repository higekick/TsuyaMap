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

public abstract class AbstractContentData {

    public String id;
    public String icon;
    public String name;

    public void importFromJson(JSONObject j){
        try {
            this.id = j.getString("id");
            this.icon = j.getString("icon");
            this.name = j.getString("name");
        } catch (JSONException ex){
            Log.d("GalleryData","unhandled Exception.", ex);
        }
    }

    public abstract AbstractContentData newInstance();

    public abstract String getFragmentClassName();
}
