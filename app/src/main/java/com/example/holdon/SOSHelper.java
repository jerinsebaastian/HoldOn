package com.example.holdon;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class SOSHelper {

    public static void triggerSOS(Context context) {
        // If overlay permission not granted, ask user
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Toast.makeText(context, "Please allow 'Display over other apps' permission.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Show cancel popup overlay
        showCancelPopup(context);
    }

    // ================== SHOW CANCEL POPUP ==================

    public static void showCancelPopup(Context context) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(() -> {
            final boolean[] isCancelled = {false};
            final int[] countdown = {5};

            android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(80, 60, 80, 60);
            layout.setBackgroundColor(android.graphics.Color.WHITE);

            android.widget.TextView title = new android.widget.TextView(context);
            title.setText("HoldOn SOS Activation");
            title.setTextSize(20);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setTextColor(Color.RED);
            layout.addView(title);

            android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, -1));
            }

            android.widget.TextView message = new android.widget.TextView(context);
            message.setTextSize(16);
            message.setTextColor(Color.BLACK);
            message.setPadding(0, 30, 0, 30);
            message.setText("SOS will be sent in 5 seconds!");
            layout.addView(message);

            android.widget.Button cancelBtn = new android.widget.Button(context);
            cancelBtn.setBackgroundColor(Color.RED);
            cancelBtn.setTextColor(Color.WHITE);
            cancelBtn.setTypeface(null, android.graphics.Typeface.BOLD);
            cancelBtn.setLetterSpacing(0.1f);
            cancelBtn.setText("CANCEL");
            layout.addView(cancelBtn);

            int LAYOUT_FLAG = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    ? android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : android.view.WindowManager.LayoutParams.TYPE_PHONE;

            android.view.WindowManager.LayoutParams params = new android.view.WindowManager.LayoutParams(
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT,
                    LAYOUT_FLAG,
                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                    android.graphics.PixelFormat.TRANSLUCENT
            );

            params.gravity = android.view.Gravity.CENTER;

            android.view.WindowManager wm = (android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            wm.addView(layout, params);

            cancelBtn.setOnClickListener(v -> {
                isCancelled[0] = true;
                wm.removeView(layout);
                Toast.makeText(context, "SOS Cancelled.", Toast.LENGTH_SHORT).show();
            });

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isCancelled[0]) return;
                    countdown[0]--;
                    if (countdown[0] > 0) {
                        message.setText("SOS will be sent in " + countdown[0] + " seconds!");
                        handler.postDelayed(this, 1000);
                    } else {
                        wm.removeView(layout);
                        Toast.makeText(context, "SOS Triggered!", Toast.LENGTH_SHORT).show();
                        sendSOS(context);
                        makeEmergencyCall(context);
                    }
                }
            }, 1000);
        });
    }

    // ================== SEND SOS WITH LOCATION + BATTERY + SIGNAL ==================
    public static void sendSOS(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("HoldOnData", Context.MODE_PRIVATE);

        final String c1 = prefs.getString("contact1", "");
        final String c2 = prefs.getString("contact2", "");
        final String c3 = prefs.getString("contact3", "");

        final String m1 = prefs.getString("message1", "I am in danger! Please help me!");
        final String m2 = prefs.getString("message2", "I am in danger! Please help me!");
        final String m3 = prefs.getString("message3", "I am in danger! Please help me!");

        // Ensure SMS + Location + READ_PHONE_STATE permission (for signal) are available
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            if (context instanceof Activity) {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                        200);
            } else {
                Toast.makeText(context, "Permissions not granted for SMS/Location", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            // make this final so nested lambda can reference it
            final String locationInfo;
            if (location != null) {
                String locationLink = "https://maps.google.com/?q=" +
                        location.getLatitude() + "," + location.getLongitude();
                String liveTrackLink = "https://www.google.com/maps/dir/?api=1&destination=" +
                        location.getLatitude() + "," + location.getLongitude();
                locationInfo = "\n📍 My Location: " + locationLink +
                        "\n🗺️ Track me here: " + liveTrackLink;
            } else {
                locationInfo = "\n📍 Location unavailable.";
            }

            // Battery percent
            final int batteryPercent = getBatteryPercentage(context);

            // Get signal asynchronously and then send SMS (locationInfo is final so it can be used here)
            getSignalStrength(context, signalLevel -> {
                String signalText = signalLevel >= 0 ? signalLevel + "/4 bars" : "Unavailable";
                String extraInfo = "\n🔋 Battery: " + batteryPercent + "%" +
                        "\n📶 Signal: " + signalText;

                if (!c1.isEmpty()) sendSMS(context, c1, m1 + locationInfo + extraInfo);
                if (!c2.isEmpty()) sendSMS(context, c2, m2 + locationInfo + extraInfo);
                if (!c3.isEmpty()) sendSMS(context, c3, m3 + locationInfo + extraInfo);
            });
        });
    }


    // ================== BATTERY LEVEL ==================
    private static int getBatteryPercentage(Context context) {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    // ================== SIGNAL STRENGTH ==================

    // Callback interface
    public interface SignalCallback {
        void onSignalMeasured(int level);
    }

    private static void getSignalStrength(Context context, SignalCallback callback) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // If READ_PHONE_STATE not granted, return -1
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                callback.onSignalMeasured(-1);
                return;
            }

            tm.listen(new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    super.onSignalStrengthsChanged(signalStrength);
                    int level = -1;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23+
                        try {
                            level = signalStrength.getLevel(); // 0..4
                        } catch (Exception e) {
                            level = -1;
                        }
                    } else {
                        // Fallback for older devices: use GSM ASU -> RSSI -> estimate bars
                        try {
                            int asu = signalStrength.getGsmSignalStrength(); // 0..31 or 99
                            if (asu == 99) {
                                level = -1;
                            } else {
                                int rssi = -113 + 2 * asu; // approximate dBm
                                if (rssi >= -70) level = 4;
                                else if (rssi >= -85) level = 3;
                                else if (rssi >= -100) level = 2;
                                else if (rssi >= -110) level = 1;
                                else level = 0;
                            }
                        } catch (Exception e) {
                            level = -1;
                        }
                    }

                    callback.onSignalMeasured(level);
                    // stop listening after we got the first value
                    tm.listen(this, PhoneStateListener.LISTEN_NONE);
                }
            }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        } catch (Exception e) {
            callback.onSignalMeasured(-1);
        }
    }


    // ================== SMS SENDER ==================
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

    // ================== EMERGENCY CALL ==================
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

    // ================== LOW BATTERY ALERT ==================
    public static void sendLowBatteryAlert(Context context, int level) {
        SharedPreferences prefs = context.getSharedPreferences("HoldOnData", Context.MODE_PRIVATE);
        String c1 = prefs.getString("contact1", "");
        String c2 = prefs.getString("contact2", "");
        String c3 = prefs.getString("contact3", "");

        String msg = "⚠️ Low Battery Alert!\nBattery: " + level + "%\nSending last known location soon...";
        sendSMS(context, c1, msg);
        sendSMS(context, c2, msg);
        sendSMS(context, c3, msg);

        sendSOS(context); // Sends last known location
    }

    // ================== Device Shutdown Detection ==================
    public static void sendShutdownAlert(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("HoldOnData", Context.MODE_PRIVATE);
        String c1 = prefs.getString("contact1", "");
        String c2 = prefs.getString("contact2", "");
        String c3 = prefs.getString("contact3", "");

        String msg = "⚠️ My phone is shutting down!\nLast known location being shared...";
        sendSMS(context, c1, msg);
        sendSMS(context, c2, msg);
        sendSMS(context, c3, msg);
        sendSOS(context); // sends last known location
    }


}
