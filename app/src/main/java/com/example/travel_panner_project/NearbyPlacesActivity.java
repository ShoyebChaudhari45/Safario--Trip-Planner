package com.example.travel_panner_project;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NearbyPlacesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private RequestQueue requestQueue;

    // Coordinates passed from RouteActivity
    private double destLat = 0, destLon = 0;

    // Search radius in metres (5 km matches screenshot "5 km")
    private static final int RADIUS_M = 5000;
    // Max results to show
    private static final int MAX_RESULTS = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_places);

        recyclerView = findViewById(R.id.nearbyPlacesList);
        progressBar  = findViewById(R.id.nearbyProgress);
        emptyView    = findViewById(R.id.nearbyEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestQueue = Volley.newRequestQueue(this);

        // Get destination coordinates from intent
        String latStr = getIntent().getStringExtra("destLat");
        String lonStr = getIntent().getStringExtra("destLon");

        if (latStr != null && lonStr != null) {
            try {
                destLat = Double.parseDouble(latStr);
                destLon = Double.parseDouble(lonStr);
                fetchNearbyPlaces();
            } catch (NumberFormatException e) {
                showEmpty();
            }
        } else {
            showEmpty();
        }
    }

    // ─── Wikipedia Geosearch ───────────────────────────────────────────────

    private void fetchNearbyPlaces() {
        showLoading(true);

        // Step 1: geosearch — get page IDs near the destination
        String geoUrl = "https://en.wikipedia.org/w/api.php"
                + "?action=query"
                + "&list=geosearch"
                + "&gscoord=" + destLat + "%7C" + destLon
                + "&gsradius=" + RADIUS_M
                + "&gslimit=" + MAX_RESULTS
                + "&format=json";

        JsonObjectRequest geoReq = new JsonObjectRequest(Request.Method.GET, geoUrl, null,
                response -> {
                    try {
                        JSONArray pages = response
                                .getJSONObject("query")
                                .getJSONArray("geosearch");

                        if (pages.length() == 0) {
                            showLoading(false);
                            showEmpty();
                            return;
                        }

                        // Build pipe-separated page-ID string for thumbnail fetch
                        StringBuilder ids = new StringBuilder();
                        List<NearbyPlace> places = new ArrayList<>();

                        for (int i = 0; i < pages.length(); i++) {
                            JSONObject p = pages.getJSONObject(i);
                            NearbyPlace place = new NearbyPlace();
                            place.pageId   = p.getInt("pageid");
                            place.title    = p.getString("title");
                            place.lat      = p.getDouble("lat");
                            place.lon      = p.getDouble("lon");
                            place.dist     = p.getInt("dist");
                            places.add(place);

                            if (ids.length() > 0) ids.append("|");
                            ids.append(place.pageId);
                        }

                        fetchThumbnails(ids.toString(), places);

                    } catch (Exception e) {
                        e.printStackTrace();
                        showLoading(false);
                        showEmpty();
                    }
                },
                error -> {
                    showLoading(false);
                    showEmpty();
                });

        requestQueue.add(geoReq);
    }

    // Step 2: fetch thumbnails + descriptions for all page IDs in one request
    private void fetchThumbnails(String pageIds, List<NearbyPlace> places) {
        String thumbUrl = "https://en.wikipedia.org/w/api.php"
                + "?action=query"
                + "&pageids=" + pageIds
                + "&prop=pageimages|description"
                + "&piprop=thumbnail"
                + "&pithumbsize=200"
                + "&format=json";

        JsonObjectRequest thumbReq = new JsonObjectRequest(Request.Method.GET, thumbUrl, null,
                response -> {
                    try {
                        JSONObject pagesObj = response
                                .getJSONObject("query")
                                .getJSONObject("pages");

                        // Map thumbnail URLs and descriptions back onto places by pageId
                        for (NearbyPlace place : places) {
                            String key = String.valueOf(place.pageId);
                            if (pagesObj.has(key)) {
                                JSONObject pg = pagesObj.getJSONObject(key);
                                if (pg.has("thumbnail")) {
                                    place.thumbUrl = pg.getJSONObject("thumbnail")
                                            .getString("source");
                                }
                                if (pg.has("description")) {
                                    place.description = pg.getString("description");
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Non-fatal — we still show the list without images
                    }

                    showLoading(false);
                    showResults(places);
                },
                error -> {
                    // Thumbnail fetch failed — still show list without images
                    showLoading(false);
                    showResults(places);
                });

        requestQueue.add(thumbReq);
    }

    // ─── UI helpers ───────────────────────────────────────────────────────

    private void showResults(List<NearbyPlace> places) {
        if (places.isEmpty()) {
            showEmpty();
            return;
        }
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        recyclerView.setAdapter(new NearbyPlacesAdapter(places));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }
}
