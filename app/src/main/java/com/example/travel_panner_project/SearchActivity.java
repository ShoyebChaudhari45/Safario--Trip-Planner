package com.example.travel_panner_project;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity {

    private AutoCompleteTextView sourceInput, destinationInput;
    private ProgressBar progressBar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_search);

        sourceInput      = findViewById(R.id.sourceInput);
        destinationInput = findViewById(R.id.destinationInput);
        progressBar      = findViewById(R.id.searchProgress);

        ImageButton btnSearch = findViewById(R.id.btnFindRoutes);
        ImageButton btnSwap   = findViewById(R.id.btnSwap);

        btnSearch.setOnClickListener(v -> searchRoutes());
        btnSwap.setOnClickListener(v -> swapInputs());

        // Profile tab
        findViewById(R.id.tabProfile).setOnClickListener(v ->
                startActivity(new Intent(SearchActivity.this, ProfileActivity.class)));

        // Featured trip cards — tap to auto-fill source/destination
        wireTripCard(R.id.tripKashmirKanyakumari, "Kashmir", "Kanyakumari");
        wireTripCard(R.id.tripMumbaiDelhi,        "Mumbai",  "Delhi");
        wireTripCard(R.id.tripBangaloreGoa,       "Bangalore", "Goa");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void wireTripCard(int cardId, String from, String to) {
        CardView card = findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(v -> {
                sourceInput.setText(from);
                destinationInput.setText(to);
                // Scroll to top so user sees the filled inputs
                sourceInput.requestFocus();
            });
        }
    }

    private void swapInputs() {
        String temp = sourceInput.getText().toString();
        sourceInput.setText(destinationInput.getText().toString());
        destinationInput.setText(temp);
    }

    private void searchRoutes() {
        String source      = sourceInput.getText().toString().trim();
        String destination = destinationInput.getText().toString().trim();

        if (source.isEmpty() || destination.isEmpty()) {
            Toast.makeText(this, "Please enter both locations", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnFindRoutes).setEnabled(false);

        executor.execute(() -> {
            double[] srcCoords  = getCoordinates(source);
            double[] destCoords = getCoordinates(destination);

            runOnUiThread(() -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                findViewById(R.id.btnFindRoutes).setEnabled(true);

                if (srcCoords == null || destCoords == null) {
                    Toast.makeText(this,
                            "Could not find location. Try a more specific name.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(SearchActivity.this, RouteActivity.class);
                intent.putExtra("sourceCity", source);
                intent.putExtra("destCity",   destination);
                intent.putExtra("sourceLat",  String.valueOf(srcCoords[0]));
                intent.putExtra("sourceLon",  String.valueOf(srcCoords[1]));
                intent.putExtra("destLat",    String.valueOf(destCoords[0]));
                intent.putExtra("destLon",    String.valueOf(destCoords[1]));
                startActivity(intent);
            });
        });
    }

    private double[] getCoordinates(String location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(location, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return new double[]{address.getLatitude(), address.getLongitude()};
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
