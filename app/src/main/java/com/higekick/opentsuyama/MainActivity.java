package com.higekick.opentsuyama;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.higekick.opentsuyama.aws.S3ClientManager;
import com.higekick.opentsuyama.util.Const;
import com.higekick.opentsuyama.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import com.amazonaws.mobile.client.AWSMobileClient;

public class MainActivity extends AppCompatActivity {

    // button to change contents
    Button btnChangeToMap;
    Button btnChangeToGallery;

    MyBroadcastReceiver mReciever;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set up drwaer view
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        setNavigationView();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // set up map fragment
        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            changeToMap();
        }

        mReciever = new MyBroadcastReceiver();
        registerReceiver(mReciever, new IntentFilter(Const.ACTION_RETRIEVE_FINISH));

        // S3ClientManager.initAWSClient(this);
        startService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mReciever != null) {
            unregisterReceiver(mReciever);
        }
    }

    private void changeToMap(){
        MainMapFragment mainMapFragment = MainMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mainMapFragment, MainMapFragment.class.getSimpleName()).commit();

        NavigationView nv = (NavigationView) findViewById(R.id.nav_view);

        Menu m = nv.getMenu();
        m.clear();

        // set left side menu from json assets
        setupSideMenu(new MapData(), nv, R.raw.map_data);
    }

    private void changeToGallery(){
        MainGalleryFragment mainGalleryFragment = MainGalleryFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mainGalleryFragment, MainGalleryFragment.class.getSimpleName()).commit();

        NavigationView nv = (NavigationView) findViewById(R.id.nav_view);

        Menu m = nv.getMenu();
        m.clear();

        // set left side menu from json assets
        // setupSideMenu(new GalleryData(), nv, R.raw.gallery_data);
        setupSideMenuFromFile(new GalleryData(), nv, R.raw.gallery_data);
    }

    private void setupSideMenuFromFile(final AbstractContentData contentData, NavigationView nv,int idJsonFile){
        Context con = this;
        String dirPathImage = con.getFilesDir().getAbsolutePath() + "/" + Const.IMG_PRFX;
        File dirFileImage = new File(dirPathImage);
        String[] dirList = dirFileImage.list();

        // setup menus
        final HashMap<Integer, AbstractContentData> mapDataHashMap;
        if (contentData instanceof MapData){
//            mapDataHashMap = setupMapMenuItem(nv,datas,contentData);
            // Todo inflate data
            mapDataHashMap = null;
        } else if (contentData instanceof GalleryData){
            SubMenu sm = nv.getMenu().addSubMenu(R.string.menu_section_gallery);
//            mapDataHashMap = setupGalleryMenuItem(sm,datas,contentData);
            mapDataHashMap = setupGalleryMenuItemFromFile(sm,dirList,contentData);
        } else {
            // nothing to do
            return;
        }

        // left side menu select action
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                AbstractContentData data = mapDataHashMap.get(id);

                IMainFragmentExecuter fragment = (IMainFragmentExecuter) getSupportFragmentManager().findFragmentByTag(contentData.getFragmentClassName());
                if (fragment != null) {
                    fragment.executeLoading(data);
                }

                setTitle(data.name);
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);

                return false;
            }
        });
    }

    private void setupSideMenu(final AbstractContentData contentData, NavigationView nv,int idJsonFile){
        JSONArray datas = null;
        try {
            datas = new JSONArray(Util.getJsonFromRawFile(idJsonFile, MainActivity.this));
        } catch (JSONException ex) {
            Log.d("MainActivity", "unhandled JsonException.", ex);
        }

        // setup menus
        final HashMap<Integer, AbstractContentData> mapDataHashMap;
        if (contentData instanceof MapData){
            mapDataHashMap = setupMapMenuItem(nv,datas,contentData);
        } else if (contentData instanceof GalleryData){
            SubMenu sm = nv.getMenu().addSubMenu(R.string.menu_section_gallery);
            mapDataHashMap = setupGalleryMenuItem(sm,datas,contentData);
//            mapDataHashMap = setupGalleryMenuItemFromFile(sm,datas,contentData);
        } else {
            // nothing to do
            return;
        }

        // left side menu select action
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                AbstractContentData data = mapDataHashMap.get(id);

                IMainFragmentExecuter fragment = (IMainFragmentExecuter) getSupportFragmentManager().findFragmentByTag(contentData.getFragmentClassName());
                if (fragment != null) {
                    fragment.executeLoading(data);
                }

                setTitle(data.name);
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);

                return false;
            }
        });
    }

    private HashMap<Integer, AbstractContentData> setupMapMenuItem(NavigationView nv, JSONArray datas, final AbstractContentData contentData){
        Menu m = nv.getMenu();
        final HashMap<Integer, AbstractContentData> mapDataHashMap = new HashMap<>();

        for (int i = 0; i <= datas.length() - 1; i++) {
            try {
                JSONObject section = datas.getJSONObject(i);
                SubMenu sm = m.addSubMenu(section.getString("section_name"));
                JSONArray places = section.getJSONArray("places");
                for (int j = 0; j <= places.length() - 1; j++) {
                    AbstractContentData data = contentData.newInstance();
                    data.importFromJson(places.getJSONObject(j));
                    int resIconId = getResources().getIdentifier(data.icon, "drawable", this.getPackageName());
                    int itemId = i*10 + j;
                    sm.add(0, itemId, itemId, data.name).setIcon(resIconId);
                    mapDataHashMap.put(itemId, data);
                }
            } catch (JSONException ex) {
                Log.d("MainActivity", "unhandled JsonException.", ex);
            }
        }
       return mapDataHashMap;
    }

    private HashMap<Integer, AbstractContentData> setupGalleryMenuItemFromFile(SubMenu sm, String[] dirs, final AbstractContentData contentData) {
        final HashMap<Integer, AbstractContentData> mapDataHashMap = new HashMap<>();

        for (int i = 0; i <= dirs.length - 1; i++) {
            AbstractContentData data = contentData.newInstance();
            data.importFromFile(this, dirs[i]);
            int resIconId = getResources().getIdentifier("ic_menu_camera", "drawable", this.getPackageName());
//                int picNum = ((GalleryData) data).picUrls.size();
            int picNum = 10;
            sm.add(0, i, i, getDirName(dirs[i]) + " (" + picNum + ")").setIcon(resIconId);
            mapDataHashMap.put(i, data);
        }
        return mapDataHashMap;
    }

    private String getDirName(String dirId){
        String pathDir = this.getFilesDir().getAbsolutePath() + "/" + Const.IMG_PRFX + "/" + dirId + "/" + "dirname.txt";
        File f = new File(pathDir);
        if (f.exists()) {

            Uri uri = Uri.fromFile(f);
            InputStream stream;
            try {
                stream = this.getContentResolver().openInputStream(uri);
                InputStreamReader inputStreamReader = new InputStreamReader(stream);
                BufferedReader bufferReader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = bufferReader.readLine()) != null) {
                    return line;
                }
            } catch (FileNotFoundException ex) {
                Log.e("LoadingImage", "failed to load image.", ex);
                return null;
            } catch (IOException ex) {
                Log.e("LoadingImage", "failed to load image.", ex);
                return null;
            }
        }
        return "";
    }

    private HashMap<Integer, AbstractContentData> setupGalleryMenuItem(SubMenu sm, JSONArray datas, final AbstractContentData contentData) {
        final HashMap<Integer, AbstractContentData> mapDataHashMap = new HashMap<>();

        for (int i = 0; i <= datas.length() - 1; i++) {
            try {
                AbstractContentData data = contentData.newInstance();
                data.importFromJson(datas.getJSONObject(i));
                int resIconId = getResources().getIdentifier(data.icon, "drawable", this.getPackageName());
                int picNum = ((GalleryData) data).picUrls.size();
                sm.add(0, i, i, data.name + " (" + picNum + ")").setIcon(resIconId);
                mapDataHashMap.put(i, data);
            } catch (JSONException ex) {
                Log.d("MainActivity", "unhandled JsonException.", ex);
            }
        }
        return mapDataHashMap;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setNavigationView() {
        NavigationView nv = (NavigationView) findViewById(R.id.nav_view);
        View headerLayout = nv.getHeaderView(0);

        // set up Button
        btnChangeToMap = (Button) headerLayout.findViewById(R.id.btnMap);
        btnChangeToGallery = (Button) headerLayout.findViewById(R.id.btnGallery);
        btnChangeToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeToMap();
            }
        });
        btnChangeToGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeToGallery();
            }
        });

    }

    private void startService(){
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo info = new JobInfo.Builder(1,new ComponentName(this, S3RetrieveJobService.class))
                .setMinimumLatency(0)
                .setOverrideDeadline(5000)
                .build();
        jobScheduler.schedule(info);
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent i) {
            changeToGallery();
        }
    }
}
