package com.higekick.opentsuyama;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
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

import com.higekick.opentsuyama.util.Const;
import com.higekick.opentsuyama.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
        implements EntranceFragment.OnOpenDrawerListener{

    // button to change contents
    Button btnChangeToMap;
    Button btnChangeToGallery;
    Button btnChangeToSettings;

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
            changeToEntrance();
        }

        mReciever = new MyBroadcastReceiver();
        registerReceiver(mReciever, new IntentFilter(Const.ACTION_RETRIEVE_FINISH));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReciever != null) {
            unregisterReceiver(mReciever);
            mReciever = null;
        }
    }

    private void changeToEntrance() {
        EntranceFragment fragment = EntranceFragment.newInstance();
        fragment.setOpenListener(this);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, EntranceFragment.class.getSimpleName()).commit();
    }

    private void changeToMap(){
        Util.startService(this,Const.JSON_PRFX);
        MainMapFragment mainMapFragment = MainMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mainMapFragment, MainMapFragment.class.getSimpleName()).commit();

        NavigationView nv = (NavigationView) findViewById(R.id.nav_view);

        Menu m = nv.getMenu();
        m.clear();

        // set left side menu from json assets
        // set left side menu from image file
        setupSideMenuFromFile(new MapData(), nv);
    }

    private void changeToGallery(){
        Util.startService(this,Const.IMG_PRFX);
        MainGalleryFragment mainGalleryFragment = MainGalleryFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mainGalleryFragment, MainGalleryFragment.class.getSimpleName()).commit();

        NavigationView nv = (NavigationView) findViewById(R.id.nav_view);

        Menu m = nv.getMenu();
        m.clear();

        // set left side menu from image file
        setupSideMenuFromFile(new GalleryData(), nv);
    }

    private void setupSideMenuFromFile(final AbstractContentData contentData, NavigationView nv){
        Context con = this;
        String dirPathImage;

        // setup menus
        final HashMap<Integer, AbstractContentData> mapDataHashMap;
        SubMenu sm;
        String path;
        if (contentData instanceof MapData){
            sm = nv.getMenu().addSubMenu(R.string.menu_section_position);
            path = Const.JSON_PRFX;
        } else if (contentData instanceof GalleryData){
            sm = nv.getMenu().addSubMenu(R.string.menu_section_gallery);
            path = Const.IMG_PRFX;
        } else {
            // nothing to do
            return;
        }
        dirPathImage = con.getFilesDir().getAbsolutePath() + "/" + path;
        File dirFileImage = new File(dirPathImage);
        String[] dirList = dirFileImage.list();
        mapDataHashMap = setupMenuItemFromFile(sm,dirList,contentData,path);

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

    private HashMap<Integer, AbstractContentData> setupMenuItemFromFile(SubMenu sm,
                                                                        String[] dirs,
                                                                        final AbstractContentData contentData,
                                                                        final String path) {
        final HashMap<Integer, AbstractContentData> mapDataHashMap = new HashMap<>();

        if (dirs != null) {
            for (int i = 0; i <= dirs.length - 1; i++) {
                if (Util.getInvisibleFile(this, dirs[i], path).exists()) {
                    // if setting invisible by setting menu, do not show.
                    continue;
                }
                AbstractContentData data = contentData.newInstance();
                String dirName = Util.getDirName(this, dirs[i], path);
                data.importFromFile(this, dirName, dirs[i]);
                int resIconId = getResources().getIdentifier("ic_menu_camera", "drawable", this.getPackageName());
                int picNum = getFileCount(dirs[i], path);
                sm.add(0, i, i, dirName + " (" + picNum + ")").setIcon(resIconId);
                mapDataHashMap.put(i, data);
            }
        }
        return mapDataHashMap;
    }

    private int getFileCount(String dirId, String path) {
        String pathDir = this.getFilesDir().getAbsolutePath() + "/" + path + "/" + dirId;
        File f = new File(pathDir);
        if (f == null) {
            return 0;
        }
        File[] list = f.listFiles();
        int count = 0;
        if (list == null) {
            return 0;
        }
        for (File f2 : list) {
            if (f2.isFile() &&
                    ( f2.getName().endsWith(".jpg") || f2.getName().endsWith(".json") )
                    ){
               count++;
            }
        }
        return count;
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

        btnChangeToSettings = (Button) headerLayout.findViewById(R.id.btnSetting);
        btnChangeToSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this,SettingsActivity.class);
                startActivity(i);
            }
        });

    }

    @Override
    public void onExecuteGalleryOpen() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.openDrawer(GravityCompat.START);
        changeToGallery();
    }

    @Override
    public void onExecuteMapOpen() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.openDrawer(GravityCompat.START);
        changeToMap();
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent i) {
            // changeToGallery();
        }
    }
}
