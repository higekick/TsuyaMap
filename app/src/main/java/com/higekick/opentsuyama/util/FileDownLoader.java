package com.higekick.opentsuyama.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.higekick.opentsuyama.BuildConfig;
import com.higekick.opentsuyama.MapData;
import com.higekick.opentsuyama.dao.LocationData;
import com.higekick.opentsuyama.dao.Masterfile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by User on 2016/12/18.
 */

public class FileDownLoader extends AsyncTask<MapData,Integer,List<LocationData>>implements DialogInterface.OnCancelListener {

    ProgressDialog pdialog ;
    GoogleMap mMap;
    WeakReference<Context> weakContext;
    OnMarkerSetupListner onMarkerSetupListner;

    public FileDownLoader(GoogleMap map,Context context, OnMarkerSetupListner listner){
        this.mMap = map;
        pdialog = new ProgressDialog(context);
        weakContext = new WeakReference<>(context);
        onMarkerSetupListner = listner;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mMap.clear();

        pdialog.cancel();
        pdialog.setTitle("Please wait");
        pdialog.setMessage("Loading data...");
        pdialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pdialog.setCancelable(true);
        pdialog.setOnCancelListener(this);
        pdialog.setMax(100);
        pdialog.setProgress(0);
        pdialog.show();

    }

    @Override
    protected List<LocationData> doInBackground(MapData... params) {
        HttpURLConnection con = null;
        InputStream inputStream;
        BufferedReader reader;

        List<LocationData> list = new ArrayList<>();

        MapData mapData = params[0];
        URL url = Util.getURL(mapData.url);
        Realm realm = Realm.getDefaultInstance();
        Masterfile masterfile = null;
        try {

            // get preserved data from local database
            Masterfile targetMaster = realm.where(Masterfile.class).equalTo("name", mapData.name).findFirst();

            if (Util.netWorkCheck(weakContext.get())) {
                //get connection
                con = (HttpURLConnection) url.openConnection();
                con.connect();
            } else if (targetMaster == null) {
                // is not connecting network nor do not have local data, can't do anything...
                return null;
            }

            Date dtLastMod = null;
            if(con != null && con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // get Last Modified Date of Server file
                String lastModified = con.getHeaderField("Last-Modified");
                SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE',' dd' 'MMM' 'yyyy HH':'mm':'ss zzz", Locale.US);
                dtLastMod = dateFormatter.parse(lastModified);
            } else if (targetMaster == null) {
                return null;
            }

            if (BuildConfig.DEBUG) {
                Log.d("modifiedDate", "server file date=" + dtLastMod == null ? "" : dtLastMod.toString());
                if (targetMaster != null) {
                    Log.d("modifiedDate", "local data date=" + targetMaster.getFileDate().toString());
                }
            }

            if (targetMaster != null && targetMaster.getFileDate().equals(dtLastMod)) {
                for (LocationData loc : targetMaster.getLocations()) {
                    list.add(realm.copyFromRealm(loc));
                }
                realm.close();
                return list;
            }

            inputStream = con.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, "Shift-JIS"));
            String line;

            realm.beginTransaction();
            masterfile = realm.createObject(Masterfile.class);
            masterfile.setName(mapData.name);
            masterfile.setFileDate(dtLastMod);
            int nextId = realm.where(Masterfile.class).max("id").intValue() + 1;
            masterfile.setId(nextId);
            int i = 0;
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                line = line.replace("\"", "");
                List<String> strList = Arrays.asList(line.split(","));
                LocationData ld = realm.createObject(LocationData.class);
                int nextLdId = realm.where(Masterfile.class).max("id").intValue() + 1;

                ld.setId(nextLdId);
                ld.setName(strList.get(0));
                if (mapData.id.equals("aedPosition")) {
                    ld.setAddress(strList.get(2));
                    ld.setTel(strList.get(3));
                    ld.setMemo(strList.get(1));
                    ld.setLocationX(Double.parseDouble(strList.get(5)));
                    ld.setLocationY(Double.parseDouble(strList.get(6)));
                } else {
                    ld.setAddress(strList.get(1));
                    ld.setTel(strList.get(2));
                    ld.setFax(strList.get(3));
                    ld.setUrl(strList.get(4));
                    ld.setMemo(strList.get(5));
                    ld.setLocationX(Double.parseDouble(strList.get(6)));
                    ld.setLocationY(Double.parseDouble(strList.get(7)));
                }
                masterfile.getLocations().add(ld);
//                    sb.append(line);
                publishProgress(i++);
            }

            realm.commitTransaction();

            for (LocationData loc : masterfile.getLocations()) {
                list.add(realm.copyFromRealm(loc));
            }
            realm.close();
            return list;
        } catch (Exception ex) {
            Toast.makeText(weakContext.get(), "データアクセス中にエラーが発生しました。",Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }finally {
            if (con != null){
                con.disconnect();
            }
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
            LatLng location = new LatLng(data.getLocationY(),data.getLocationX());
            Marker marker = mMap.addMarker(new MarkerOptions().position(location).title(data.getName()));
            mapMarkerData.put(marker,data);
            centerXSum += data.getLocationX();
            centerYSum += data.getLocationY();
        }

        LatLng center = new LatLng(centerYSum/ datas.size(),centerXSum/datas.size());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center,10));

        onMarkerSetupListner.onMarkerSetup(mapMarkerData);
        pdialog.dismiss();
    }

    @Override protected void onCancelled() {
        pdialog.dismiss();
    }

    @Override public void onCancel(DialogInterface dialog) {
        this.cancel(true);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        pdialog.setProgress(values[0]);
    }

    public interface OnMarkerSetupListner {
        void onMarkerSetup(HashMap<Marker, LocationData> markerLocationDataHashMap);
    }
}
