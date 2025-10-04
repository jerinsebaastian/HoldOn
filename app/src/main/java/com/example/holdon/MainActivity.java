package com.example.holdon;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sosBtn = findViewById(R.id.sosBtn);
        ImageButton settingsBtn = findViewById(R.id.settingsBtn);

        // SOS button click
        sosBtn.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                SOSHelper.triggerSOS(MainActivity.this);
            } else {
                requestAllPermissions();
            }
        });

        // Settings button click
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // If triggered by AccessibilityService
        if (getIntent().getBooleanExtra("triggerSOS", false)) {
            new android.os.Handler().postDelayed(() -> {
                if (hasAllPermissions()) {
                    SOSHelper.triggerSOS(MainActivity.this);
                } else {
                    requestAllPermissions();
                }
            }, 1000);
        }
    }

    private boolean hasAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CALL_PHONE},
                PERMISSION_REQUEST_CODE);
    }

    // Handle runtime permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasAllPermissions()) {
                SOSHelper.triggerSOS(this);
            } else {
                Toast.makeText(this, "Permissions required for SOS!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
