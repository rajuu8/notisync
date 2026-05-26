package com.notisync.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.*;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ConnectActivity extends AppCompatActivity {

    private EditText etCode;
    private Button btnConnect, btnScan;
    private static final int CAMERA_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        etCode     = findViewById(R.id.et_code);
        btnConnect = findViewById(R.id.btn_connect);
        btnScan    = findViewById(R.id.btn_scan);

        // Already connected?
        String savedCode = getSharedPreferences("notisync", MODE_PRIVATE)
                .getString("user_code", null);
        if (savedCode != null) { startMain(); return; }

        btnConnect.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            if (code.length() != 4) {
                Toast.makeText(this, "4 digit code daalo!", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyCode(code);
        });

        btnScan.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            } else {
                startQRScan();
            }
        });
    }

    private void startQRScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Notification Dekho app ka QR scan karo");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scannedCode = result.getContents().trim();
                if (scannedCode.length() == 4) {
                    verifyCode(scannedCode);
                } else {
                    Toast.makeText(this, "Invalid QR! Dobara try karo.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startQRScan();
        } else {
            Toast.makeText(this, "Camera permission chahiye QR scan ke liye!", Toast.LENGTH_SHORT).show();
        }
    }

    private void verifyCode(String code) {
        btnConnect.setEnabled(false);
        btnScan.setEnabled(false);
        btnConnect.setText("Check ho raha hai...");

        FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("codes").child(code).get()
        .addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                getSharedPreferences("notisync", MODE_PRIVATE)
                    .edit().putString("user_code", code).apply();
                Toast.makeText(this, "Connected! ✅", Toast.LENGTH_SHORT).show();
                startMain();
            } else {
                Toast.makeText(this, "Code galat hai!", Toast.LENGTH_SHORT).show();
                btnConnect.setEnabled(true);
                btnScan.setEnabled(true);
                btnConnect.setText("Connect Karo");
            }
        })
        .addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnConnect.setEnabled(true);
            btnScan.setEnabled(true);
            btnConnect.setText("Connect Karo");
        });
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
