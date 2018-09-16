package com.higekick.opentsuyama.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.higekick.opentsuyama.BuildConfig;
import com.higekick.opentsuyama.MapData;
import com.higekick.opentsuyama.dao.LocationData;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;

/**
 * Created by User on 2016/12/18.
 */

public class FileDownLoader extends AsyncTask<MapData,Integer,List<LocationData>>implements DialogInterface.OnCancelListener {

    GoogleMap mMap;
    WeakReference<Context> weakContext;
    OnMarkerSetupListner onMarkerSetupListner;

    public FileDownLoader(GoogleMap map,Context context, OnMarkerSetupListner listner){
        this.mMap = map;
        weakContext = new WeakReference<>(context);
        onMarkerSetupListner = listner;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mMap.clear();
    }

    @Override
    protected List<LocationData> doInBackground(MapData... params) {
        List<LocationData> list = new ArrayList<>();

        try {
            MapData mapData = params[0];
            String dirPath = Util.getJsonDirPath(weakContext.get()) + "/" + mapData.id;
            File dirObj = new File(dirPath);
            if (dirObj == null) {
                return null;
            }
            File[] files = dirObj.listFiles();
            for (File f : files) {
                if (f.getName().equals("dirname.txt")) {
                    continue;
                }
                JSONArray datas = Util.getJsonFromFile(f, weakContext.get());
                if (datas == null || datas.length() == 0) {
                    continue;
                }
                for (int i=0;i<=datas.length()-1;i++) {
                    LocationData ld = new LocationData();
                    JSONObject j = datas.getJSONObject(i);
                    ld.importFromJson(j);
                    if (!TextUtils.isEmpty(ld.getName())){
                        list.add(ld);
                    }
                }
            }
            return list;
        } catch (Exception ex) {
            Toast.makeText(weakContext.get(), "ファイル読込中にエラーが発生しました。",Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }finally {
        }
        return null;
    }

    HashMap<Marker, LocationData> mapMarkerData = new HashMap<>();

    @Override
    protected void onPostExecute(List<LocationData> datas) {
        if (datas == null){
            Toast.makeText(weakContext.get(), "ネットワークに接続されている状態で再度メニューを選択してください。",Toast.LENGTH_LONG).show();
            return;
        }

        double centerXSum = 0;
        double centerYSum = 0;
        mapMarkerData.clear();
        for (LocationData data: datas) {
            LatLng location = new LatLng(data.getAlt(),data.getLon());
            Marker marker = mMap.addMarker(new MarkerOptions().position(location).title(data.getName()));
            mapMarkerData.put(marker,data);
            centerXSum += data.getLon();
            centerYSum += data.getAlt();
        }

        LatLng center = new LatLng(centerYSum/ datas.size(),centerXSum/datas.size());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center,10));

        onMarkerSetupListner.onMarkerSetup(mapMarkerData);
    }

    @Override protected void onCancelled() {
    }

    @Override public void onCancel(DialogInterface dialog) {
        this.cancel(true);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
    }

    public interface OnMarkerSetupListner {
        void onMarkerSetup(HashMap<Marker, LocationData> markerLocationDataHashMap);
    }
}
