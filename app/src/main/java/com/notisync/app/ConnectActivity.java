package com.notisync.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

public class ConnectActivity extends AppCompatActivity {

    private EditText etCode;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        etCode    = findViewById(R.id.et_code);
        btnConnect = findViewById(R.id.btn_connect);

        // Pehle se connected hai?
        String savedCode = getSharedPreferences("notisync", MODE_PRIVATE)
                .getString("user_code", null);
        if (savedCode != null) {
            // Seedha main activity pe jao
            startMain();
            return;
        }

        btnConnect.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            if (code.length() != 4) {
                Toast.makeText(this, "4 digit code daalo!", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyCode(code);
        });
    }

    private void verifyCode(String code) {
        btnConnect.setEnabled(false);
        btnConnect.setText("Check ho raha hai...");

        FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("codes").child(code).get()
        .addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // Code sahi hai — save karo
                getSharedPreferences("notisync", MODE_PRIVATE)
                    .edit().putString("user_code", code).apply();
                Toast.makeText(this, "Connected! ✅", Toast.LENGTH_SHORT).show();
                startMain();
            } else {
                Toast.makeText(this, "Code galat hai! Dobara try karo.", Toast.LENGTH_SHORT).show();
                btnConnect.setEnabled(true);
                btnConnect.setText("Connect Karo");
            }
        })
        .addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnConnect.setEnabled(true);
            btnConnect.setText("Connect Karo");
        });
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
