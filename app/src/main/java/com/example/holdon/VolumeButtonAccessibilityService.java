package com.example.holdon;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class VolumeButtonAccessibilityService extends AccessibilityService {

    private static long lastPressTime = 0;
    private static int pressCount = 0;

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN &&
                (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN ||
                        event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)) {

            long currentTime = System.currentTimeMillis();

            // Count presses within 2 seconds gap
            if (currentTime - lastPressTime < 2000) {
                pressCount++;
            } else {
                pressCount = 1;
            }
            lastPressTime = currentTime;

            if (pressCount >= 5) {
                pressCount = 0;
                new Thread(() -> SOSHelper.triggerSOS(getApplicationContext())).start();
            }
        }
        return super.onKeyEvent(event);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used
    }

    @Override
    public void onInterrupt() {
        // Not used
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "HoldOnSOSChannel",
                    "HoldOn SOS Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, "HoldOnSOSChannel")
                    .setContentTitle("HoldOn Active")
                    .setContentText("Listening for volume button SOS trigger...")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setColor(Color.RED)
                    .build();

            startForeground(1, notification);
        }

        Toast.makeText(this, "HoldOn SOS Service Active", Toast.LENGTH_SHORT).show();
    }

}
