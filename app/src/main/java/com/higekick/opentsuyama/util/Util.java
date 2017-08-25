package com.higekick.opentsuyama.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.higekick.opentsuyama.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
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

    public static String getJsonFromRawFile(int resId, Context con) {
        InputStream is = con.getResources().openRawResource(resId);
        Writer w = new StringWriter();
        try{
            BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = r.readLine();
            while (line != null) {
                w.write(line);
                line = r.readLine();
            }
        } catch (Exception ex){
            Log.e("ReadingJson","Fatal Unhandled Exception",ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex){
                Log.e("ReadingJson","Fatal Unhandled Exception",ex);
            }
        }

        return w.toString();
    }

    // ネットワーク接続確認
    public static boolean netWorkCheck(Context context){
        ConnectivityManager cm =  (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if( info != null ){
            return info.isConnected();
        } else {
            return false;
        }
    }

}
