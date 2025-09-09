package com.example.holdon;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Location;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    EditText contact1, contact2, contact3, message;
    Button saveBtn;
    Button sendBtn;
    private static final int SMS_PERMISSION_CODE = 101;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_CODE = 102;

    private void sendSOS() {
        SharedPreferences prefs = getSharedPreferences("HoldOnData", MODE_PRIVATE);

        // Make everything final (for lambda use)
        final String c1 = prefs.getString("contact1", "");
        final String c2 = prefs.getString("contact2", "");
        final String c3 = prefs.getString("contact3", "");
        String savedMsg = prefs.getString("message", "");
        final String baseMsg = (savedMsg == null || savedMsg.trim().isEmpty())
                ? "I am in danger! Please help me!"
                : savedMsg.trim();

        // Permission check
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }

        // Try last known location first
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                // Build message with last known location
                String locationLink = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();

                String liveTrackLink = "https://www.google.com/maps/dir/?api=1&destination=" +
                        location.getLatitude() + "," + location.getLongitude() + "&travelmode=driving";


                String finalMsg = baseMsg
                        + "\n\n\nMy Last Known Location: " + locationLink
                        + "\n\nTrack me on Google Maps Live: " + liveTrackLink;
                sendSMS(c1, c2, c3, finalMsg);
            } else {
                // If lastLocation is null, request a fresh one
                fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                ).addOnSuccessListener(loc -> {
                    String finalMsg = baseMsg;
                    if (loc != null) {
                        String locationLink = "https://maps.google.com/?q=" +
                                location.getLatitude() + "," + location.getLongitude();

                        String liveTrackLink = "https://www.google.com/maps/dir/?api=1&destination=" +
                                loc.getLatitude() + "," + loc.getLongitude() + "&travelmode=driving";

                        // No new declaration → just assign to finalMsg
                        finalMsg = baseMsg
                                + "\n\n\nMy Last Known Location: " + locationLink
                                + "\n\nTrack me on Google Maps Live: " + liveTrackLink;

                    } else {
                        finalMsg += "\nLocation unavailable right now.";
                    }
                    sendSMS(c1, c2, c3, finalMsg);
                });
            }
        });
    }


    private void sendSMS(String c1, String c2, String c3, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        try {
            ArrayList<String> parts = smsManager.divideMessage(message);
            if (!c1.isEmpty()) smsManager.sendMultipartTextMessage(c1, null, parts, null, null);
            if (!c2.isEmpty()) smsManager.sendMultipartTextMessage(c2, null, parts, null, null);
            if (!c3.isEmpty()) smsManager.sendMultipartTextMessage(c3, null, parts, null, null);
            Toast.makeText(this, "SOS sent with location!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void makeEmergencyCall() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CALL_PHONE}, 300);
        } else {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(android.net.Uri.parse("tel:112"));
            startActivity(callIntent);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSOS();
            } else {
                Toast.makeText(this, "SMS Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSOS(); // Retry sending SOS after permission granted
            } else {
                Toast.makeText(this, "Location Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contact1 = findViewById(R.id.contact1);
        contact2 = findViewById(R.id.contact2);
        contact3 = findViewById(R.id.contact3);
        message = findViewById(R.id.message);
        saveBtn = findViewById(R.id.saveBtn);
        sendBtn = findViewById(R.id.sendBtn);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        SharedPreferences prefs = getSharedPreferences("HoldOnData", MODE_PRIVATE);

        // Load saved data if exists
        contact1.setText(prefs.getString("contact1", ""));
        contact2.setText(prefs.getString("contact2", ""));
        contact3.setText(prefs.getString("contact3", ""));
        message.setText(prefs.getString("message", ""));

        if (getIntent().getBooleanExtra("triggerSOS", false)) {
            sendSOS(); // auto-send SOS when triggered
            makeEmergencyCall();
        }

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("contact1", contact1.getText().toString());
                editor.putString("contact2", contact2.getText().toString());
                editor.putString("contact3", contact3.getText().toString());
                editor.putString("message", message.getText().toString());
                editor.apply();

                // Show confirmation
                Toast.makeText(MainActivity.this, "Emergency details saved successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check SMS permission
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
                } else {
                    sendSOS();
                }
            }
        });

    }
}
