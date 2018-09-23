package com.higekick.opentsuyama;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;

import com.higekick.opentsuyama.util.Const;
import com.higekick.opentsuyama.util.ProgressDialogCustome;
import com.higekick.opentsuyama.util.Util;

import java.io.File;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
        implements EntranceFragment.OnOpenDrawerListener
        ,ProgressDialogCustome.OnDownloadFinishListener
        ,MainGalleryFragment.OnFragmentInteractionListener {
    public static int REQUEST_CODE_SETTING = 1001;

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
            reload();
        }

        mReciever = new MyBroadcastReceiver();
        registerReceiver(mReciever, new IntentFilter(Const.ACTION_RETRIEVE_FINISH));
    }

    private void reload() {
        int currentUse = Util.getIntPreferenceValue(this, Const.KEY_CURRENT_USE);
        if (currentUse == Const.CURRENT_USE_IMAGE) {
            changeToGallery();
        } else if (currentUse == Const.CURRENT_USE_MAP) {
            changeToMap();
        } else {
            Intent intent = new Intent(this, IntroductionActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SETTING) {
            if (resultCode == RESULT_OK) {
                reload();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReciever != null) {
            unregisterReceiver(mReciever);
            mReciever = null;
        }
    }

    private void changeToMap(){
        tryDownload(Const.JSON_PRFX);

        MainMapFragment mainMapFragment = MainMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mainMapFragment, MainMapFragment.class.getSimpleName()).commit();

        // set left side menu from json assets
        // set left side menu from image file
        setupSideMenuFromFile(new MapData());
    }

    private void changeToGallery(){
        tryDownload(Const.IMG_PRFX);

        MainGalleryFragment mainGalleryFragment = MainGalleryFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mainGalleryFragment, MainGalleryFragment.class.getSimpleName()).commit();

        // set left side menu from image file
        setupSideMenuFromFile(new GalleryData());
    }

    private void tryDownload(String prfx) {
        if (Util.getBooleanPreferenceValue(this, Const.KEY_DOWNLOAD_X + prfx)) {
            // すでにダウンロードしている
            return;
        }
        if (!Util.netWorkCheck(this)) {
            // ダウンロード必要だが、ネットワーク接続がない
            return;
        }
        DownloadFragment fragment = new DownloadFragment();
        fragment.show(getSupportFragmentManager(), "tag");
        fragment.setOnDownloadFinishListener(this);
        Util.startService(this, prfx);
    }

    private void setupSideMenuFromFile(final AbstractContentData contentData){
        NavigationView nv = findViewById(R.id.nav_view);
        Menu m = nv.getMenu();
        m.clear();

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
                int resIconId;
                String itemName;
                int picNum = Util.getFileCount(this, dirs[i], path);
                if (picNum == 0) {
                    continue;
                }
                if (path.equals(Const.JSON_PRFX)) {
                    resIconId = getResources().getIdentifier("ic_menu_gallery", "drawable", this.getPackageName());
                    itemName = dirName;
                } else {
                    resIconId = getResources().getIdentifier("ic_menu_camera", "drawable", this.getPackageName());
                    itemName = dirName + " (" + picNum + ")";
                }
                sm.add(0, i, i, itemName).setIcon(resIconId);
                mapDataHashMap.put(i, data);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

//        if (id == R.id.action_settings) {
//            return true;
//        }
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
                Util.setPreferenceValue(MainActivity.this, Const.KEY_CURRENT_USE, Const.CURRENT_USE_MAP);
                changeToMap();
            }
        });
        btnChangeToGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.setPreferenceValue(MainActivity.this, Const.KEY_CURRENT_USE, Const.CURRENT_USE_IMAGE);
                changeToGallery();
            }
        });

        btnChangeToSettings = (Button) headerLayout.findViewById(R.id.btnSetting);
        btnChangeToSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this,SettingsActivity.class);
                startActivityForResult(i, REQUEST_CODE_SETTING);
            }
        });
    }

    @Override
    public void onExecuteGalleryOpen() {
        openDrawer();
        changeToGallery();
    }

    @Override
    public void onExecuteMapOpen() {
        openDrawer();
        changeToMap();
    }

    private void openDrawer() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.openDrawer(GravityCompat.START);
    }

    @Override
    public void onDownloadFinish() {
        openDrawer();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    @Override
    public void onSetOptionItemVisibility(@IdRes int idMenu, boolean ifVisible) {
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent i) {
            // changeToGallery();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        } else {
            openDrawer();
            return false;
        }
    }
}
