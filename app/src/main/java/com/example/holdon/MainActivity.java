package com.example.holdon;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.work.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String PREFS_NAME = "HoldOnPrefs";
    private static final String KEY_BATTERY_PROMPT_SHOWN = "batteryPromptShown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sosBtn = findViewById(R.id.sosBtn);
        ImageButton settingsBtn = findViewById(R.id.settingsBtn);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "Please allow 'Display over other apps' permission for HoldOn.", Toast.LENGTH_LONG).show();
            }
        }

        // ======== ONBOARDING CHECK (Runs Only Once) ========
        // Run onboarding only when needed
        SharedPreferences prefs = getSharedPreferences("HoldOnSetup", MODE_PRIVATE);
        boolean setupDone = prefs.getBoolean("setupDone", false);

        if (!setupDone || !isAccessibilityServiceEnabled()) {
            showSetupDialog();
        }

        // Save setup as done (so dialog doesn’t repeat unless accessibility is off)
        if (isAccessibilityServiceEnabled()) {
            prefs.edit().putBoolean("setupDone", true).apply();
        }


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

    // ===================== ONBOARDING METHODS =====================
    private void showSetupDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Enable Essential Permissions")
                .setMessage("For HoldOn to protect you effectively, please:\n\n" +
                        "1️⃣ Enable Accessibility Service (for volume button trigger)\n" +
                        "2️⃣ Allow Battery Optimization Exception\n\n" +
                        "Would you like to set this up now?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (!isAccessibilityServiceEnabled()) {
                        openAccessibilitySettings();
                        new android.os.Handler().postDelayed(this::openBatteryOptimizationSettings, 3000);
                    } else {
                        openBatteryOptimizationSettings();
                    }
                })
                .setNegativeButton("Later", (dialog, which) ->
                        Toast.makeText(this, "You can enable them later from settings.", Toast.LENGTH_SHORT).show()
                )
                .show();
    }

    // ✅ Check if Accessibility Service is enabled
    private boolean isAccessibilityServiceEnabled() {
        android.view.accessibility.AccessibilityManager am =
                (android.view.accessibility.AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (am != null) {
            for (android.accessibilityservice.AccessibilityServiceInfo service :
                    am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
                if (service.getId().contains(getPackageName() + "/.VolumeButtonAccessibilityService")) {
                    return true;
                }
            }
        }
        return false;
    }

    // ✅ Open Accessibility Settings
    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "Scroll to 'HoldOn SOS' and enable the service.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open Accessibility settings.", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Open Battery Optimization Settings
    private void openBatteryOptimizationSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "Find HoldOn and allow 'No restrictions' for best performance.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open Battery Optimization settings.", Toast.LENGTH_SHORT).show();
        }
    }

    // ===================== CANCEL OPTION =====================


    // ===================== PERMISSIONS =====================
    private boolean hasAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_PHONE_STATE },
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

    // Periodic Location Sharing
    private void startPeriodicLocationUpdates() {
        PeriodicWorkRequest locationWork =
                new PeriodicWorkRequest.Builder(LocationWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "HoldOnLocationUpdates",
                ExistingPeriodicWorkPolicy.KEEP,
                locationWork
        );
    }
}
