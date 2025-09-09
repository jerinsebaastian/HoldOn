package com.example.holdon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class PowerButtonReceiver extends BroadcastReceiver {

    private static long lastPressTime = 0;
    private static int pressCount = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()) ||
                Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {

            long currentTime = System.currentTimeMillis();

            // If presses are within 2 seconds, count them
            if (currentTime - lastPressTime < 2000) {
                pressCount++;
            } else {
                pressCount = 1; // Reset if too slow
            }

            lastPressTime = currentTime;

            if (pressCount >= 3) {  // 3 quick presses
                pressCount = 0; // reset counter
                Toast.makeText(context, "SOS Triggered!", Toast.LENGTH_SHORT).show();

                // Start MainActivity to send SOS
                Intent sosIntent = new Intent(context, MainActivity.class);
                sosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sosIntent.putExtra("triggerSOS", true);
                context.startActivity(sosIntent);
            }
        }
    }
}
