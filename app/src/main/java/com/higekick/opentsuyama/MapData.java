package com.higekick.opentsuyama;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by User on 2017/08/21.
 */

public class MapData extends AbstractContentData{

    public String url;

    @Override
    public void importFromJson(JSONObject j){
        try {
            super.importFromJson(j);
            this.url = j.getString("url");
        } catch (JSONException ex){
            Log.d("MapData","unhandled Exception.", ex);
        }
    }

    @Override
    public AbstractContentData newInstance() {
        return new MapData();
    }

    @Override
    public String getFragmentClassName(){
        return MainMapFragment.class.getSimpleName();
    }

}
