package com.higekick.opentsuyama;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.exceptions.RealmMigrationNeededException;

/**
 * Created by User on 2016/12/20.
 */

public class OpenTsuyama extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initAction();
    }

    private void initAction() {
        // Create a RealmConfiguration that saves the Realm file in the app's "files" directory.
        RealmConfiguration realmConfig;
        try{
            Realm.init(getApplicationContext());
            realmConfig = new RealmConfiguration.Builder().deleteRealmIfMigrationNeeded()
                    .schemaVersion(0)
                    .build();
        }catch (RealmMigrationNeededException e){
            realmConfig = new RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()
                    .build();
        }
        Realm.setDefaultConfiguration(realmConfig);
    }

}
