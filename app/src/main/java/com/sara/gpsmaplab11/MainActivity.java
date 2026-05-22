package com.sara.gpsmaplab11;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;

    private MapView mapView;
    private TextView tvCoords;
    private LocationManager locationManager;
    private Marker currentMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid config obligatoire avant setContentView
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        mapView  = findViewById(R.id.mapView);
        tvCoords = findViewById(R.id.tvCoords);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Config carte OSM
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Position initiale : Marrakech
        GeoPoint marrakech = new GeoPoint(31.6295, -7.9811);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(marrakech);

        // Vérifier permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void startLocationUpdates() {
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                // Mettre à jour le TextView
                tvCoords.setText("Latitude : " + lat + "  |  Longitude : " + lng);

                // Déplacer la carte
                GeoPoint point = new GeoPoint(lat, lng);
                mapView.getController().animateTo(point);
                mapView.getController().setZoom(15.0);

                // Un seul marker qui bouge
                if (currentMarker != null) {
                    mapView.getOverlays().remove(currentMarker);
                }
                currentMarker = new Marker(mapView);
                currentMarker.setPosition(point);
                currentMarker.setTitle("Ma position");
                currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(currentMarker);
                mapView.invalidate(); // Rafraîchir la carte

                Toast.makeText(getApplicationContext(),
                        "Lat: " + lat + "\nLng: " + lng,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                buildAlertMessageNoGps();
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                Toast.makeText(getApplicationContext(),
                        "GPS activé !", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
        };

        // Écouter GPS + Réseau
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 50, listener);
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000, 50, listener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void buildAlertMessageNoGps() {
        new AlertDialog.Builder(this)
                .setMessage("Votre GPS semble désactivé. Voulez-vous l'activer ?")
                .setCancelable(false)
                .setPositiveButton("Oui", (dialog, id) ->
                        startActivity(new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                )
                .setNegativeButton("Non", (dialog, id) -> dialog.cancel())
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Permission GPS refusée !", Toast.LENGTH_LONG).show();
        }
    }

    // Cycle de vie obligatoire pour OSMDroid
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}