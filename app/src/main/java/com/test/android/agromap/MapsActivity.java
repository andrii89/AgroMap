package com.test.android.agromap;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.PolyUtil;
import java.util.LinkedList;
import java.util.List;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback{

    private static final String TAG = "State";
    final private int REQUEST_CODE_ASK_PERMISSIONS = 1;

    private GoogleMap mMap;
    Marker marker;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location userLocation;
    private Polygon mPolygon;
    FloatingActionButton mFloatingActionButton;

    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

    private boolean mPolygonFlag;

    private List<LatLng> polygonPoints = new LinkedList<>();

    long UPDATE_INTERVAL = 10000;
    long FASTEST_INTERVAL = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mPolygonFlag = false;

        mFloatingActionButton = findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mPolygonFlag){
                    Toast.makeText(getApplicationContext(), R.string.polygon_draw_toast, Toast.LENGTH_LONG).show();
                    mPolygonFlag = true;
                } else {
                    polygonPoints.clear();
                    if(mPolygon != null){
                        mPolygon.remove();
                    }

                }

            }
        });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    userLocation = location;
                    Log.v(TAG,"Location is defined");
                    if (marker == null){
                        marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(location.getLatitude(), location.getLongitude())));
                        LatLng locationCoordinates = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationCoordinates,15));
                    } else {
                        marker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
                    }
                }
            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
            @Override
            public void onPolygonClick(Polygon polygon) {
                LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                if(PolyUtil.containsLocation(userLatLng, polygonPoints, false)){
                    Intent intent = new Intent(getApplicationContext(), ReportActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.polygon_report_toast, Toast.LENGTH_LONG).show();
                }

            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            public void onMapClick(LatLng point){
                if (mPolygonFlag){
                    polygonPoints.add(point);
                    if(!polygonPoints.isEmpty()){
                        if(mPolygon != null){
                            mPolygon.remove();
                        }
                        mPolygon = mMap.addPolygon(new PolygonOptions()
                                .clickable(true)
                                .addAll(polygonPoints)
                                .fillColor(0xffF57F17));
                    }
                }
            }
        });
    }

    public  boolean isLocationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                        mLocationCallback,
                        null);
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showMessageOKCancel(MapsActivity.this.getApplicationContext().getResources()
                                    .getString(R.string.location_explanation_toast),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MapsActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                            REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            });
                    return false;
                }
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_ASK_PERMISSIONS);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null);
            return true;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MapsActivity.this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, okListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isLocationPermissionGranted();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
}
