package com.higekick.opentsuyama.util;

import android.content.Context;

import com.higekick.opentsuyama.R;

import java.net.URL;

/**
 * Created by User on 2016/12/19.
 */

public class Util {

    public static URL getURL(String urlpath){
        URL url=null;
        try {
            url = new URL(urlpath);
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return url;
    }

}
