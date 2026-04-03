package com.example.travel_panner_project;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RouteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView routeInfo;
    private String startLat, startLon, endLat, endLon, sourceCity, destinationCity;
    private RequestQueue requestQueue;
    private GoogleMap mMap;
    private MapView smallMapView;
    private LinearLayout transportOptionsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_route);

        routeInfo                = findViewById(R.id.routeInfo);
        smallMapView             = findViewById(R.id.smallMapView);
        FloatingActionButton fab = findViewById(R.id.fullScreenButton);
        transportOptionsContainer = findViewById(R.id.transportOptionsContainer);
        Button btnNearby = findViewById(R.id.btnViewNearbyPlaces);
        Button btnHotel  = findViewById(R.id.btnHotelBooking);
        requestQueue = Volley.newRequestQueue(this);

        startLat        = getIntent().getStringExtra("sourceLat");
        startLon        = getIntent().getStringExtra("sourceLon");
        endLat          = getIntent().getStringExtra("destLat");
        endLon          = getIntent().getStringExtra("destLon");
        sourceCity      = getIntent().getStringExtra("sourceCity");
        destinationCity = getIntent().getStringExtra("destCity");

        btnHotel.setOnClickListener(v -> openHotelBooking());
        btnNearby.setOnClickListener(v -> openNearbyPlaces());

        View.OnClickListener openFullScreen = v -> openFullScreenMap();
        smallMapView.setOnClickListener(openFullScreen);
        fab.setOnClickListener(openFullScreen);

        if (startLat != null && startLon != null && endLat != null && endLon != null) {
            fetchRoute();
        } else {
            routeInfo.setText("Invalid location data received.");
        }

        smallMapView.onCreate(savedInstanceState);
        smallMapView.getMapAsync(this);

        loadTransportOptions();
    }

    // ── Navigation helpers ──────────────────────────────────────────────────

    private void openHotelBooking() {
        Intent intent = new Intent(this, HotelBookingActivity.class);
        intent.putExtra("destCity", destinationCity);
        startActivity(intent);
    }

    private void openNearbyPlaces() {
        Intent intent = new Intent(this, NearbyPlacesActivity.class);
        intent.putExtra("destLat", endLat);
        intent.putExtra("destLon", endLon);
        startActivity(intent);
    }

    private void openFullScreenMap() {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("sourceLat", startLat);
        intent.putExtra("sourceLon", startLon);
        intent.putExtra("destLat",   endLat);
        intent.putExtra("destLon",   endLon);
        startActivity(intent);
    }

    // ── Map ─────────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Guard: coordinates may be null if intent extras were missing
        if (startLat == null || startLon == null || endLat == null || endLon == null) return;

        LatLng source      = new LatLng(Double.parseDouble(startLat), Double.parseDouble(startLon));
        LatLng destination = new LatLng(Double.parseDouble(endLat),   Double.parseDouble(endLon));

        mMap.addMarker(new MarkerOptions().position(source).title("Source"));
        mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(source, 10));

        fetchRouteOnMap();
    }

    private void fetchRouteOnMap() {
        String url = buildOsrmUrl();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (isFinishing() || isDestroyed() || mMap == null) return;
                    try {
                        JSONArray coords = response.getJSONArray("routes")
                                .getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONArray("coordinates");
                        List<LatLng> points = new ArrayList<>();
                        for (int i = 0; i < coords.length(); i++) {
                            JSONArray pt = coords.getJSONArray(i);
                            points.add(new LatLng(pt.getDouble(1), pt.getDouble(0)));
                        }
                        mMap.addPolyline(new PolylineOptions()
                                .addAll(points).width(5)
                                .color(Color.parseColor("#2B2D8C")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                Throwable::printStackTrace);
        requestQueue.add(request);
    }

    // ── Route info ──────────────────────────────────────────────────────────

    private void fetchRoute() {
        String url = buildOsrmUrl();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject route = response.getJSONArray("routes").getJSONObject(0);
                        double km  = route.getDouble("distance") / 1000;
                        double min = route.getDouble("duration") / 60;
                        routeInfo.setText(
                                sourceCity + "  →  " + destinationCity
                                + "\n\nDistance:  " + String.format("%.1f", km) + " km"
                                + "\nDuration:  " + String.format("%.0f", min) + " mins");
                    } catch (Exception e) {
                        routeInfo.setText("Could not parse route.");
                    }
                },
                error -> routeInfo.setText("Error fetching route. Check your connection."));
        requestQueue.add(request);
    }

    private String buildOsrmUrl() {
        return "https://router.project-osrm.org/route/v1/driving/"
                + startLon + "," + startLat + ";"
                + endLon   + "," + endLat
                + "?overview=full&geometries=geojson";
    }

    // ── Transport options ───────────────────────────────────────────────────

    private void loadTransportOptions() {
        addTransportOption(R.drawable.ic_bus,    "Bus",    false);
        addTransportOption(R.drawable.ic_train,  "Train",  true);
        addTransportOption(R.drawable.ic_flight, "Flight", false);
    }

    private void addTransportOption(int iconRes, String label, boolean isTrain) {
        View item = getLayoutInflater().inflate(
                R.layout.transport_option_item, transportOptionsContainer, false);
        ((ImageView) item.findViewById(R.id.transportIcon)).setImageResource(iconRes);
        ((TextView)  item.findViewById(R.id.transportName)).setText(label);

        item.setOnClickListener(v -> {
            if (isTrain) {
                Intent intent = new Intent(this, TransportDetailsActivity.class);
                intent.putExtra("source",      sourceCity);
                intent.putExtra("destination", destinationCity);
                startActivity(intent);
            } else {
                String url;
                if (label.equals("Bus")) {
                    url = "https://www.redbus.in/search?fromCityName="
                            + Uri.encode(sourceCity)
                            + "&toCityName=" + Uri.encode(destinationCity);
                } else {
                    url = "https://www.makemytrip.com/flights/";
                }
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        transportOptionsContainer.addView(item);
    }

    // ── MapView lifecycle ───────────────────────────────────────────────────

    @Override protected void onResume()  { super.onResume();  smallMapView.onResume(); }
    @Override protected void onPause()   { super.onPause();   smallMapView.onPause(); }
    @Override protected void onStop()    { super.onStop();    smallMapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); smallMapView.onDestroy(); }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        smallMapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        smallMapView.onSaveInstanceState(outState);
    }
}
