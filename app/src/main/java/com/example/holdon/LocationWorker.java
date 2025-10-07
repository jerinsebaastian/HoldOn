package com.example.holdon;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.telephony.SmsManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class LocationWorker extends Worker {

    public LocationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(getApplicationContext());

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("HoldOnData", Context.MODE_PRIVATE);
        String c1 = prefs.getString("contact1", "");
        String c2 = prefs.getString("contact2", "");
        String c3 = prefs.getString("contact3", "");

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String link = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                String msg = "📍 Live Location Update:\n" + link + "\nTime: " + java.text.DateFormat.getTimeInstance().format(new java.util.Date());
                sendSMS(c1, msg);
                sendSMS(c2, msg);
                sendSMS(c3, msg);
            }
        });

        return Result.success();
    }

    private void sendSMS(String number, String message) {
        if (number == null || number.isEmpty()) return;
        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(number, null, parts, null, null);
        } catch (Exception ignored) { }
    }
}
