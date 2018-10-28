package com.higekick.opentsuyama;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.higekick.opentsuyama.dao.LocationData;
import com.higekick.opentsuyama.util.FileDownLoader;

import java.util.HashMap;

public class MainMapFragment extends Fragment implements OnMapReadyCallback
        ,GoogleMap.OnMarkerClickListener
        ,FileDownLoader.OnMarkerSetupListner
        ,GoogleMap.OnMapClickListener
        ,IMainFragmentExecuter
{

    GoogleMap gMap;
    private static final int REQUEST_PERMISSION_LOCATION = 2001;

    final LatLng centerOfTsuyama = new LatLng(35.069104, 134.004542);

    //Views
    CardView viewDetail;
    FloatingActionButton fabFocusOnTsuyama;
    TextView txtPlaceName;
    TextView txtAddress;
    TextView txtMemo;
    TextView txtTel;
    TextView txtUrl;

    public MainMapFragment() {
        // Required empty public constructor
    }

    public static MainMapFragment newInstance() {
        MainMapFragment fragment = new MainMapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_map, container, false);

        // setup views
        viewDetail = view.findViewById(R.id.detail_view);
        fabFocusOnTsuyama = view.findViewById(R.id.fabFocusOnTsuyama);
        txtPlaceName = view.findViewById(R.id.txt_place_name);
        txtAddress = view.findViewById(R.id.txt_address);
        txtMemo = view.findViewById(R.id.txt_memo);
        txtTel = view.findViewById(R.id.txt_tel);
        txtUrl = view.findViewById(R.id.txt_url);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        // Get the button view
        View locationButton = ((View) mapFragment.getView().findViewById(1).getParent()).findViewById(2);

        // and next place it, for exemple, on bottom right (as Google Maps app)
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.setMargins(0, 0, 30, 30);

        mapFragment.getMapAsync(this);

        view.findViewById(R.id.detail_view).setVisibility(View.GONE);

        view.findViewById(R.id.fabFocusOnTsuyama).setOnClickListener( (v) ->
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerOfTsuyama, 10))
        );

        Activity activity = getActivity();
        if (activity!=null) {
            activity.setTitle(R.string.menu_section_position);
        }
        return view;
    }

    private Menu mMenu;
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main, menu);
        mMenu = menu;
        menu.findItem(R.id.action_map).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_map) {
            startGoogleMapFromMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startGoogleMapFromMap() {
        if (selectedLocationData == null) {return;}
        String lat = convertStringLatAt(selectedLocationData);
        // Default google map
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(
                "http://maps.google.com/maps?z=12&q=loc:" + lat + "(" + selectedLocationData.getName() + ")" + "&hnear=" + lat));
        startActivity(intent);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMenu.findItem(R.id.action_map).setVisible(false);
    }

    @Override
    public void executeLoading(AbstractContentData data) {
        this.selectedMap = (MapData) data;
        Activity activity = getActivity();
        if (activity!=null) {
            activity.setTitle(data.name);
        }

        viewDetail.setVisibility(View.GONE);
        if (mMenu!=null) {
            mMenu.findItem(R.id.action_map).setVisible(false);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (gMap != null) {
                gMap.setMyLocationEnabled(true);
            }
        } else {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // RuntimePermissionはMashmallow以上なので
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
            }
        }
    }

    MapData selectedMap;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnMapClickListener(this);

        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        if (selectedMap == null || TextUtils.isEmpty(selectedMap.id)) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerOfTsuyama, 10));
        } else {
            FileDownLoader loader = new FileDownLoader(googleMap, getContext(), this);
            loader.execute(selectedMap);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_PERMISSION_LOCATION) {
                gMap.setMyLocationEnabled(true);
            }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(getContext(), R.string.message_request_location, Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (mapMarkerData == null || mapMarkerData.size() <= 0){
          viewDetail.setVisibility(View.GONE);
            mMenu.findItem(R.id.action_map).setVisible(false);
            return false;
        } else {
            mMenu.findItem(R.id.action_map).setVisible(true);
            viewDetail.setVisibility(View.VISIBLE);
            fabFocusOnTsuyama.setVisibility(View.INVISIBLE);
            selectedMarker = marker;
        }

        selectedLocationData = mapMarkerData.get(marker);
        if (selectedLocationData == null) {return false;}

        txtPlaceName.setText(selectedLocationData.getName());
        txtAddress.setText(selectedLocationData.getAddress());
        txtMemo.setText(selectedLocationData.getMemo());
        txtTel.setText(selectedLocationData.getTel());
        txtUrl.setText(selectedLocationData.getUrl());

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
        fabFocusOnTsuyama.setVisibility(View.VISIBLE);
        viewDetail.setVisibility(View.GONE);
        mMenu.findItem(R.id.action_map).setVisible(false);
    }

    private String convertStringLatAt(@NonNull LocationData data){
        return Double.toString(selectedLocationData.getAlt()) + ", " + Double.toString(selectedLocationData.getLon());
    }

}
