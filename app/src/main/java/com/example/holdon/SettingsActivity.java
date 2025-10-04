package com.example.holdon;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    EditText contact1, contact2, contact3;
    EditText message1, message2, message3;
    Button saveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize UI elements
        contact1 = findViewById(R.id.contact1);
        contact2 = findViewById(R.id.contact2);
        contact3 = findViewById(R.id.contact3);

        message1 = findViewById(R.id.message1);
        message2 = findViewById(R.id.message2);
        message3 = findViewById(R.id.message3);

        saveBtn = findViewById(R.id.saveBtn);

        // Load saved data (if any)
        loadSavedData();

        // Save button click
        saveBtn.setOnClickListener(v -> {
            saveData();
        });
    }

    private void loadSavedData() {
        SharedPreferences prefs = getSharedPreferences("HoldOnData", MODE_PRIVATE);

        contact1.setText(prefs.getString("contact1", ""));
        contact2.setText(prefs.getString("contact2", ""));
        contact3.setText(prefs.getString("contact3", ""));

        message1.setText(prefs.getString("message1", ""));
        message2.setText(prefs.getString("message2", ""));
        message3.setText(prefs.getString("message3", ""));
    }

    private void saveData() {
        SharedPreferences.Editor editor = getSharedPreferences("HoldOnData", MODE_PRIVATE).edit();

        editor.putString("contact1", contact1.getText().toString().trim());
        editor.putString("contact2", contact2.getText().toString().trim());
        editor.putString("contact3", contact3.getText().toString().trim());

        editor.putString("message1", message1.getText().toString().trim());
        editor.putString("message2", message2.getText().toString().trim());
        editor.putString("message3", message3.getText().toString().trim());

        editor.apply();

        Toast.makeText(this, "Details saved successfully!", Toast.LENGTH_SHORT).show();
    }
}
