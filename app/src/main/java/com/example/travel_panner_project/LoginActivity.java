package com.example.travel_panner_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText email, password;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // Already logged in — skip straight to search
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, SearchActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        db       = FirebaseFirestore.getInstance();
        email    = findViewById(R.id.email);
        password = findViewById(R.id.password);

        Button btnLogin  = findViewById(R.id.btnLogin);
        Button btnSignup = findViewById(R.id.btnSignup);

        btnLogin.setOnClickListener(v -> loginUser());
        btnSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
    }

    private void loginUser() {
        String userEmail = email.getText().toString().trim();
        String userPass  = password.getText().toString().trim();

        if (TextUtils.isEmpty(userEmail) || TextUtils.isEmpty(userPass)) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(userEmail, userPass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this, "Login failed. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        fetchUserData(user.getUid());
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserData(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String name = snapshot.getString("name");
                        Toast.makeText(this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                    }
                    startActivity(new Intent(LoginActivity.this, SearchActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Firestore fetch failed but auth succeeded — still let them in
                    Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, SearchActivity.class));
                    finish();
                });
    }
}
