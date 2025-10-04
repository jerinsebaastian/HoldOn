package com.example.holdon;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class SOSHelper {

    public static void triggerSOS(Context context) {
        sendSOS(context);
        makeEmergencyCall(context);
    }

    // Send SMS with location
    public static void sendSOS(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("HoldOnData", Context.MODE_PRIVATE);

        final String c1 = prefs.getString("contact1", "");
        final String c2 = prefs.getString("contact2", "");
        final String c3 = prefs.getString("contact3", "");

        final String m1 = prefs.getString("message1", "I am in danger! Please help me!");
        final String m2 = prefs.getString("message2", "I am in danger! Please help me!");
        final String m3 = prefs.getString("message3", "I am in danger! Please help me!");

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            if (context instanceof Activity) {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION},
                        200);
            } else {
                Toast.makeText(context, "Permissions not granted for SMS/Location", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            String locationInfo = "";
            if (location != null) {
                String locationLink = "https://maps.google.com/?q=" +
                        location.getLatitude() + "," + location.getLongitude();
                String liveTrackLink = "https://www.google.com/maps/dir/?api=1&destination=" +
                        location.getLatitude() + "," + location.getLongitude();
                locationInfo = "\nMy Location: " + locationLink + "\nTrack me here: " + liveTrackLink;
            } else {
                locationInfo = "\nLocation unavailable.";
            }

            if (!c1.isEmpty()) sendSMS(context, c1, m1 + locationInfo);
            if (!c2.isEmpty()) sendSMS(context, c2, m2 + locationInfo);
            if (!c3.isEmpty()) sendSMS(context, c3, m3 + locationInfo);
        });
    }

    // SMS Helper
    private static void sendSMS(Context context, String number, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(number, null, parts, null, null);
            Toast.makeText(context, "SOS sent to " + number, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Emergency Call
    public static void makeEmergencyCall(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:112"));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
        } else {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:112"));
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(dialIntent);
        }
    }
}

