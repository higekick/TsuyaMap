package com.higekick.opentsuyama;

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

import com.higekick.opentsuyama.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import com.amazonaws.mobile.client.AWSMobileClient;

public class MainActivity extends AppCompatActivity {

    // button to change contents
    Button btnChangeToMap;
    Button btnChangeToGallery;

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

        AWSMobileClient.getInstance().initialize(this).execute();
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
        setupSideMenu(new GalleryData(), nv, R.raw.gallery_data);
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

}
