package com.higekick.opentsuyama.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.higekick.opentsuyama.dao.LocationData;
import com.higekick.opentsuyama.dao.Masterfile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by User on 2016/12/18.
 */

public class FileDownLoader extends AsyncTask<URL,Integer,List<LocationData>>implements DialogInterface.OnCancelListener {

    ProgressDialog pdialog ;
    GoogleMap mMap;

    public FileDownLoader(GoogleMap map,Context context){
        this.mMap = map;
        pdialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mMap.clear();
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
    protected List<LocationData> doInBackground(URL... params) {
        HttpURLConnection con = null;
        InputStream inputStream;
        BufferedReader reader;
        URL url = params[0];
        Realm realm = Realm.getDefaultInstance();
        Masterfile masterfile = null;
        realm.beginTransaction();
        try {
            //get connection
            con = (HttpURLConnection) url.openConnection();
            con.connect();

            if(con.getResponseCode() == HttpURLConnection.HTTP_OK){
                inputStream = con.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream,"Shift-JIS"));
                String[] pathElements = url.getPath().split("/");
                String fileName = pathElements[pathElements.length - 1];
                String lastModified = con.getHeaderField("Last-Modified");
                SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE',' dd' 'MMM' 'yyyy HH':'mm':'ss zzz", Locale.US);
                Date dtLastMod = dateFormatter.parse(lastModified);
                String line;
                masterfile =realm.createObject(Masterfile.class);
                masterfile.setName(fileName);
                masterfile.setFileDate(dtLastMod);
                int nextId = realm.where(Masterfile.class).max("id").intValue() + 1;
                masterfile.setId(nextId);
                int i = 0;
                reader.readLine();
                while ((line = reader.readLine()) != null){
                    line = line.replace("\"","");
                    List<String> strList = Arrays.asList(line.split(","));
                    LocationData ld = realm.createObject(LocationData.class);
                    int nextLdId = realm.where(Masterfile.class).max("id").intValue() + 1;
                    ld.setId(nextLdId);
                    ld.setName(strList.get(0));
                    ld.setAddress(strList.get(1));
                    ld.setTel(strList.get(2));
                    ld.setFax(strList.get(3));
                    ld.setUrl(strList.get(4));
                    ld.setMemo(strList.get(5));
                    ld.setLocationX(Double.parseDouble(strList.get(6)));
                    ld.setLocationY(Double.parseDouble(strList.get(7)));
                    masterfile.getLocations().add(ld);
//                    sb.append(line);
                    publishProgress(i++);
                }
            }
            realm.commitTransaction();
            StringBuilder sbr = new StringBuilder();
            for (LocationData ld: masterfile.getLocations()) {
                sbr.append(ld.getName() + "\t");
                sbr.append(ld.getAddress() + "\t");
                sbr.append(ld.getTel() + "\t");
                sbr.append(System.getProperty("line.separator"));
            }
            List<LocationData> list = new ArrayList<>();
            for(LocationData loc: masterfile.getLocations()) {
                list.add(realm.copyFromRealm(loc));
            }
            realm.close();
            return list;
        }catch (Exception ex){
            ex.printStackTrace();
        }finally {
            if (con != null){
                con.disconnect();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<LocationData> datas) {
        if (datas == null){return;}

        double centerXSum = 0;
        double centerYSum = 0;
        for (LocationData data: datas) {
            LatLng location = new LatLng(data.getLocationY(),data.getLocationX());
            mMap.addMarker(new MarkerOptions().position(location).title(data.getName()));
            centerXSum += data.getLocationX();
            centerYSum += data.getLocationY();
        }

        LatLng center = new LatLng(centerYSum/ datas.size(),centerXSum/datas.size());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center,10));

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
}
