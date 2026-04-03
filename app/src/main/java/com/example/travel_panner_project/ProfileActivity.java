package com.example.travel_panner_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileEmail, tvAvatarInitial;
    private TextView tvName, tvEmail, tvMobile, tvCurrency;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Supported currencies — cycle through on tap
    private static final String[] CURRENCIES = {"INR ₹", "USD $", "EUR €", "GBP £", "JPY ¥", "AED د.إ"};
    private int currencyIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        tvProfileName   = findViewById(R.id.tvProfileName);
        tvProfileEmail  = findViewById(R.id.tvProfileEmail);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvName          = findViewById(R.id.tvName);
        tvEmail         = findViewById(R.id.tvEmail);
        tvMobile        = findViewById(R.id.tvMobile);
        tvCurrency      = findViewById(R.id.tvCurrency);

        ImageButton btnBack = findViewById(R.id.btnBack);
        LinearLayout rowLogout  = findViewById(R.id.rowLogout);
        LinearLayout rowCurrency = findViewById(R.id.rowCurrency);
        LinearLayout rowAbout   = findViewById(R.id.rowAbout);

        btnBack.setOnClickListener(v -> finish());

        rowLogout.setOnClickListener(v -> confirmLogout());

        rowCurrency.setOnClickListener(v -> {
            currencyIndex = (currencyIndex + 1) % CURRENCIES.length;
            tvCurrency.setText(CURRENCIES[currencyIndex]);
            Toast.makeText(this, "Currency set to " + CURRENCIES[currencyIndex], Toast.LENGTH_SHORT).show();
        });

        rowAbout.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Safario")
                        .setMessage("Safario — Your Smart Travel Planner\nVersion 1.0\n\nPlan trips, explore routes, find nearby places and book hotels all in one app.")
                        .setPositiveButton("OK", null)
                        .show());

        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String email = user.getEmail() != null ? user.getEmail() : "—";
        tvProfileEmail.setText(email);
        tvEmail.setText(email);

        // Load name/mobile from Firestore
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String name   = doc.getString("name");
                    String mobile = doc.getString("mobile");

                    if (name != null && !name.isEmpty()) {
                        tvProfileName.setText(name);
                        tvName.setText(name);
                        // First letter as avatar initial
                        tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    } else {
                        String emailName = email.contains("@") ? email.split("@")[0] : email;
                        tvProfileName.setText(emailName);
                        tvName.setText(emailName);
                        tvAvatarInitial.setText(emailName.isEmpty() ? "U"
                                : String.valueOf(emailName.charAt(0)).toUpperCase());
                    }

                    tvMobile.setText(mobile != null && !mobile.isEmpty() ? mobile : "—");
                })
                .addOnFailureListener(e -> {
                    // Fallback to email-derived display name
                    String emailName = email.contains("@") ? email.split("@")[0] : email;
                    tvProfileName.setText(emailName);
                    tvName.setText(emailName);
                    tvAvatarInitial.setText(emailName.isEmpty() ? "U"
                            : String.valueOf(emailName.charAt(0)).toUpperCase());
                    tvMobile.setText("—");
                });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    auth.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
