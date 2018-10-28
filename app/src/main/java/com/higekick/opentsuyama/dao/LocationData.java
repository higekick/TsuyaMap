package com.higekick.opentsuyama.dao;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by User on 2016/12/20.
 */

public class LocationData {

    private int id;
    private String name;
    private String address;
    private String tel;
    private String fax;
    private String url;
    private String memo;
    private double lon;
    private double alt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getAlt() {
        return alt;
    }

    public void setAlt(double alt) {
        this.alt = alt;
    }

    public void importFromJson(JSONObject j) throws JSONException {
        try{
            this.id = 0; //Todo set id if need
            this.name = j.getString("name");
            this.address = j.getString("address");
            this.tel = j.getString("tel");
            this.fax = j.getString("fax");
            this.url = j.getString("url");
            this.memo = j.getString("memo");
            this.lon = Double.parseDouble(j.getString("lon"));
            this.alt = Double.parseDouble(j.getString("alt"));
        } catch (Exception ex) {
            Log.e("Tsuyamap", "fatal", ex);
        }
    }
}
