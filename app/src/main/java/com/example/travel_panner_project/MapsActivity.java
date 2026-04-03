package com.example.travel_panner_project;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String startLat, startLon, endLat, endLon;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        startLat = getIntent().getStringExtra("sourceLat");
        startLon = getIntent().getStringExtra("sourceLon");
        endLat   = getIntent().getStringExtra("destLat");
        endLon   = getIntent().getStringExtra("destLon");

        requestQueue = Volley.newRequestQueue(this);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.fullscreenMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Back button — wire up here so mMap null-safety is contained to map callbacks
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Map type buttons — guard against mMap being null before map is ready
        Button btnNormal    = findViewById(R.id.btnNormal);
        Button btnSatellite = findViewById(R.id.btnSatellite);
        Button btnTerrain   = findViewById(R.id.btnTerrain);
        Button btnHybrid    = findViewById(R.id.btnHybrid);

        btnNormal.setOnClickListener(v -> {
            if (mMap != null) mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        });
        btnSatellite.setOnClickListener(v -> {
            if (mMap != null) mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        });
        btnTerrain.setOnClickListener(v -> {
            if (mMap != null) mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        });
        btnHybrid.setOnClickListener(v -> {
            if (mMap != null) mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Guard: if coordinates weren't passed, show a toast and bail
        if (startLat == null || startLon == null || endLat == null || endLon == null) {
            Toast.makeText(this, "Location data unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLng source      = new LatLng(Double.parseDouble(startLat), Double.parseDouble(startLon));
        LatLng destination = new LatLng(Double.parseDouble(endLat),   Double.parseDouble(endLon));

        mMap.addMarker(new MarkerOptions().position(source).title("Source"));
        mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(source, 10));

        fetchRoute(source, destination);
    }

    private void fetchRoute(LatLng source, LatLng destination) {
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + source.longitude + "," + source.latitude + ";"
                + destination.longitude + "," + destination.latitude
                + "?overview=full&geometries=geojson";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    // Activity may have been destroyed while request was in flight
                    if (isFinishing() || isDestroyed() || mMap == null) return;
                    try {
                        JSONArray routes = response.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONArray coordinates = routes.getJSONObject(0)
                                    .getJSONObject("geometry")
                                    .getJSONArray("coordinates");
                            List<LatLng> points = new ArrayList<>();
                            for (int i = 0; i < coordinates.length(); i++) {
                                JSONArray pt = coordinates.getJSONArray(i);
                                points.add(new LatLng(pt.getDouble(1), pt.getDouble(0)));
                            }
                            mMap.addPolyline(new PolylineOptions()
                                    .addAll(points)
                                    .width(10)
                                    .color(Color.parseColor("#2B2D8C")));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Failed to load route", Toast.LENGTH_SHORT).show();
                    }
                });

        requestQueue.add(request);
    }
}
