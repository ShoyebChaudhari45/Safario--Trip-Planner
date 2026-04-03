package com.example.travel_panner_project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TransportDetailsActivity extends AppCompatActivity {

    private TextView trainDetails;
    private EditText etFromCode, etToCode;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_details);

        trainDetails = findViewById(R.id.trainDetails);
        etFromCode   = findViewById(R.id.etFromCode);
        etToCode     = findViewById(R.id.etToCode);
        Button btnSearch = findViewById(R.id.btnSearch);

        requestQueue = Volley.newRequestQueue(this);

        // Pre-fill from intent if available (city names shown as hints only)
        String sourceCity = getIntent().getStringExtra("source");
        String destCity   = getIntent().getStringExtra("destination");
        if (sourceCity != null) etFromCode.setHint("e.g. NDLS  (from " + sourceCity + ")");
        if (destCity   != null) etToCode.setHint("e.g. CSTM  (to " + destCity + ")");

        btnSearch.setOnClickListener(v -> {
            String from = etFromCode.getText().toString().trim().toUpperCase();
            String to   = etToCode.getText().toString().trim().toUpperCase();

            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(this, "Please enter both station codes", Toast.LENGTH_SHORT).show();
                return;
            }
            trainDetails.setText("Searching trains from " + from + " to " + to + "...");
            fetchTrains(from, to);
        });
    }

    private void fetchTrains(String from, String to) {
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String url = "https://irctc1.p.rapidapi.com/api/v3/trainBetweenStations"
                + "?fromStationCode=" + from
                + "&toStationCode=" + to
                + "&dateOfJourney=" + today;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                this::parseAndDisplay,
                error -> trainDetails.setText("Failed to fetch trains.\nCheck your internet connection or verify the station codes.")) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-rapidapi-key", "b47dddbf4amshb91ca5a3b98edeep1c37a8jsn864f059597ad");
                headers.put("x-rapidapi-host", "irctc1.p.rapidapi.com");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void parseAndDisplay(JSONObject response) {
        try {
            if (!response.optBoolean("status", false)) {
                trainDetails.setText("No trains found.\nCheck the station codes and try again.");
                return;
            }

            JSONArray data = response.optJSONArray("data");
            if (data == null || data.length() == 0) {
                trainDetails.setText("No trains available for today on this route.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(data.length()).append(" train(s)\n\n");

            for (int i = 0; i < data.length(); i++) {
                JSONObject train = data.getJSONObject(i);
                sb.append(train.optString("train_name", "N/A"))
                  .append("  [").append(train.optString("train_num", "")).append("]\n");
                sb.append("Dep: ").append(train.optString("from_std", "N/A"))
                  .append("  →  Arr: ").append(train.optString("to_std", "N/A")).append("\n");
                sb.append("Duration: ").append(train.optString("duration", "N/A")).append("\n");
                sb.append("From: ").append(train.optString("from_station_name", "")).append("\n");
                sb.append("To:   ").append(train.optString("to_station_name", "")).append("\n");
                sb.append("─────────────────────\n");
            }

            trainDetails.setText(sb.toString());

        } catch (Exception e) {
            trainDetails.setText("Error reading train data.");
            e.printStackTrace();
        }
    }
}
