package com.example.travel_panner_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // If user is already authenticated, go straight to Search
            boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
            Class<?> next = loggedIn ? SearchActivity.class : LoginActivity.class;
            startActivity(new Intent(MainActivity.this, next));
            finish();
        }, 3000);
    }
}
