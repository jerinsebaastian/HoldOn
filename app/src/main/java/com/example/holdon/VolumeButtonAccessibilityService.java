package com.example.holdon;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class VolumeButtonAccessibilityService extends AccessibilityService {

    private static long lastPressTime = 0;
    private static int pressCount = 0;

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN &&
                (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN ||
                        event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)) {

            long currentTime = System.currentTimeMillis();

            if (currentTime - lastPressTime < 2000) {
                pressCount++;
            } else {
                pressCount = 1;
            }
            lastPressTime = currentTime;

            if (pressCount >= 5) {
                pressCount = 0;
                Toast.makeText(this, "SOS Triggered by Volume Button!", Toast.LENGTH_SHORT).show();
                SOSHelper.triggerSOS(this);
            }
        }
        return super.onKeyEvent(event);
    }

    @Override
    public void onInterrupt() {
        // Nothing needed
    }



    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this, "Volume Button Service Active", Toast.LENGTH_SHORT).show();
    }
}
