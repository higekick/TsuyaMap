package com.higekick.opentsuyama.dao;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by User on 2016/12/20.
 */

public class Masterfile extends RealmObject {

    private int id;
    private String name;
    private Date fileDate;
//    private RealmList<LocationData> locations;

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

    public Date getFileDate() {
        return fileDate;
    }

    public void setFileDate(Date fileDate) {
        this.fileDate = fileDate;
    }

//    public RealmList<LocationData> getLocations() {
//        return locations;
//    }
//
//    public void setLocations(RealmList<LocationData> locations) {
//        this.locations = locations;
//    }
}
