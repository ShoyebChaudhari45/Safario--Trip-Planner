package com.example.travel_panner_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText email, password, name, mobileNumber;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Minimum password length — must match the hint in activity_signup.xml ("Min. 8 characters")
    private static final int MIN_PASSWORD_LENGTH = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        name         = findViewById(R.id.name);
        email        = findViewById(R.id.email);
        password     = findViewById(R.id.password);
        mobileNumber = findViewById(R.id.mobile_no);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String userName   = name.getText().toString().trim();
        String userMobile = mobileNumber.getText().toString().trim();
        String userEmail  = email.getText().toString().trim();
        String userPass   = password.getText().toString().trim();

        if (TextUtils.isEmpty(userName) || TextUtils.isEmpty(userEmail)
                || TextUtils.isEmpty(userPass) || TextUtils.isEmpty(userMobile)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userPass.length() < MIN_PASSWORD_LENGTH) {
            Toast.makeText(this,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(userEmail, userPass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), userName, userEmail, userMobile);
                        }
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        Log.e("SignupActivity", "Error: " + msg);
                    }
                });
    }

    private void saveUserToFirestore(String uid, String userName,
                                     String userEmail, String userMobile) {
        Map<String, Object> user = new HashMap<>();
        user.put("name",          userName);
        user.put("email",         userEmail);
        user.put("mobile_number", userMobile);

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Firestore Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("SignupActivity", "Firestore Error: " + e.getMessage());
                });
    }
}
