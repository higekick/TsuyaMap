package com.higekick.opentsuyama;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by User on 2017/08/21.
 */

public class MapData {

    public String id;
    public String icon;
    public String name;
    public String url;

    public void importFromJson(JSONObject j){
        try {
            this.id = j.getString("id");
            this.icon = j.getString("icon");
            this.name = j.getString("name");
            this.url = j.getString("url");
        } catch (JSONException ex){
            Log.d("MapData","unhandled Exception.", ex);
        }
    }

}
