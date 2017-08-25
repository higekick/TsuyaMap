package com.higekick.opentsuyama;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.higekick.opentsuyama.dao.LocationData;
import com.higekick.opentsuyama.util.FileDownLoader;
import com.higekick.opentsuyama.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback
                                                                ,GoogleMap.OnMarkerClickListener
                                                                ,FileDownLoader.OnMarkerSetupListner
                                                                ,GoogleMap.OnMapClickListener{

    GoogleMap gMap;

    final LatLng centerOfTsuyama = new LatLng(35.069104, 134.004542);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        setNavigationView();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        // Get the button view
        View locationButton = ((View) mapFragment.getView().findViewById(1).getParent()).findViewById(2);

        // and next place it, for exemple, on bottom right (as Google Maps app)
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.setMargins(0, 0, 30, 30);

        mapFragment.getMapAsync(this);

        findViewById(R.id.detail_view).setVisibility(View.GONE);

        findViewById(R.id.btn_map).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedLocationData == null) {return;}
                String lat = convertStringLatAt(selectedLocationData);
                // Default google map
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(
                        "http://maps.google.com/maps?z=12&q=loc:" + lat + "(" + selectedLocationData.getName() + ")" + "&hnear=" + lat));
                startActivity(intent);
            }
        });

        findViewById(R.id.fabFocusOnTsuyama).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerOfTsuyama, 10));
            }
        });
    }

    private String convertStringLatAt(@NonNull LocationData data){
        return Double.toString(selectedLocationData.getLocationY()) + ", " + Double.toString(selectedLocationData.getLocationX());
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setNavigationView() {
        Log.d("openTsuyama", "setNavigationView");
        NavigationView nv = (NavigationView) findViewById(R.id.nav_view);
        Menu m = nv.getMenu();
        SubMenu sm = m.addSubMenu(R.string.menu_section_position);

        // set left side menu from json assets
        JSONArray datas=null;
        try {
            datas = new JSONArray(Util.getJsonFromRawFile(R.raw.map_data, MainActivity.this));
        } catch (JSONException ex){
            Log.d("MainActivity", "unhandled JsonException.", ex);
        }
        final HashMap<Integer, MapData> mapDataHashMap = new HashMap<>();
        for(int i= 0; i<= datas.length() -1; i++){
            try {
                MapData mapData = new MapData();
                mapData.importFromJson(datas.getJSONObject(i));
                int resIconId = getResources().getIdentifier(mapData.icon,"drawable",this.getPackageName());
                sm.add(0, i,i, mapData.name).setIcon(resIconId);
                mapDataHashMap.put(i,mapData);
            } catch ( JSONException ex){
                Log.d("MainActivity", "unhandled JsonException.", ex);
            }

        }

        // left side menu select action
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                MapData mapData = mapDataHashMap.get(id);

                executeLoading(mapData);

                setTitle(mapData.name);
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                findViewById(R.id.detail_view).setVisibility(View.GONE);

                return false;
            }
        });
    }

    private void executeLoading(MapData data) {
        this.selectedMap = data;
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    MapData selectedMap;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnMapClickListener(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        if (selectedMap == null || TextUtils.isEmpty(selectedMap.url)) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerOfTsuyama, 10));
        } else {
            FileDownLoader loader = new FileDownLoader(googleMap, this, this);
            loader.execute(selectedMap);
        }

}

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (mapMarkerData == null || mapMarkerData.size() <= 0){
            findViewById(R.id.detail_view).setVisibility(View.GONE);
            return false;
        } else {
            findViewById(R.id.detail_view).setVisibility(View.VISIBLE);
            findViewById(R.id.fabFocusOnTsuyama).setVisibility(View.INVISIBLE);
            selectedMarker = marker;
        }

        selectedLocationData = mapMarkerData.get(marker);
        if (selectedLocationData == null) {return false;}

        ((TextView) findViewById(R.id.txt_place_name)).setText(selectedLocationData.getName());
        ((TextView) findViewById(R.id.txt_address)).setText(selectedLocationData.getAddress());
        ((TextView) findViewById(R.id.txt_memo)).setText(selectedLocationData.getMemo());
        ((TextView) findViewById(R.id.txt_tel)).setText(selectedLocationData.getTel());
        ((TextView) findViewById(R.id.txt_url)).setText(selectedLocationData.getUrl());

        return false;
    }

    HashMap<Marker, LocationData> mapMarkerData = new HashMap<>();
    Marker selectedMarker;
    LocationData selectedLocationData;

    @Override
    public void onMarkerSetup(HashMap<Marker, LocationData> markerLocationDataHashMap) {
        mapMarkerData = markerLocationDataHashMap;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        findViewById(R.id.fabFocusOnTsuyama).setVisibility(View.VISIBLE);
        findViewById(R.id.detail_view).setVisibility(View.GONE);
    }
}
