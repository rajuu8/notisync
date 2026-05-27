package com.notisync.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.*;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvPermStatus, tvCount, tvConnected, tvAudioStatus;
    private Button btnGivePermission, btnClearAll, btnDisconnect, btnAllowAudio;
    private View statusDot;
    private DatabaseReference dbRef, audioRef;
    private String userCode;
    private static final int MIC_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userCode = getSharedPreferences("notisync", MODE_PRIVATE)
                .getString("user_code", null);
        if (userCode == null) {
            startActivity(new Intent(this, ConnectActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        tvStatus      = findViewById(R.id.tv_status);
        tvPermStatus  = findViewById(R.id.tv_perm_status);
        tvCount       = findViewById(R.id.tv_count);
        tvConnected   = findViewById(R.id.tv_connected);
        tvAudioStatus = findViewById(R.id.tv_audio_status);
        statusDot     = findViewById(R.id.status_dot);
        btnGivePermission = findViewById(R.id.btn_give_permission);
        btnClearAll   = findViewById(R.id.btn_clear_all);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnAllowAudio = findViewById(R.id.btn_allow_audio);

        tvConnected.setText("Code: " + userCode);

        dbRef    = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("users").child(userCode).child("notifications");

        audioRef = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("users").child(userCode).child("audio");

        // Mic permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
        } else {
            startAudioService();
        }

        btnGivePermission.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            Toast.makeText(this, "NotiSync ko allow karo!", Toast.LENGTH_LONG).show();
        });

        btnClearAll.setOnClickListener(v -> {
            dbRef.removeValue();
            tvCount.setText("0");
            Toast.makeText(this, "Clear!", Toast.LENGTH_SHORT).show();
        });

        btnDisconnect.setOnClickListener(v -> {
            getSharedPreferences("notisync", MODE_PRIVATE).edit()
                .remove("user_code").apply();
            stopService(new Intent(this, AudioService.class));
            startActivity(new Intent(this, ConnectActivity.class));
            finish();
        });

        // Allow audio button — jab phone 2 request kare
        btnAllowAudio.setOnClickListener(v -> {
            audioRef.child("request").setValue("start");
            btnAllowAudio.setText("Streaming... 🎙️");
            btnAllowAudio.setEnabled(false);
        });

        // Audio request listener
        audioRef.child("request").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String req = snapshot.getValue(String.class);
                if ("start".equals(req)) {
                    tvAudioStatus.setText("🎙️ Phone 2 sun raha hai");
                    tvAudioStatus.setTextColor(0xFF16A34A);
                    btnAllowAudio.setText("Stop Streaming");
                    btnAllowAudio.setEnabled(true);
                    btnAllowAudio.setOnClickListener(v -> {
                        audioRef.child("request").setValue("stop");
                    });
                } else if ("stop".equals(req) || "idle".equals(req)) {
                    tvAudioStatus.setText("Audio ready — Phone 2 se request aane do");
                    tvAudioStatus.setTextColor(0xFF888888);
                    btnAllowAudio.setText("Allow Audio");
                    btnAllowAudio.setEnabled(true);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });

        // Notification count
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                tvCount.setText(String.valueOf(snapshot.getChildrenCount()));
                tvStatus.setText("Firebase connected");
                statusDot.setBackgroundResource(R.drawable.dot_green);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                statusDot.setBackgroundResource(R.drawable.dot_red);
            }
        });
    }

    private void startAudioService() {
        Intent intent = new Intent(this, AudioService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == MIC_PERMISSION && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            startAudioService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvPermStatus != null) updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        boolean granted = isNotificationListenerEnabled();
        if (granted) {
            tvPermStatus.setText("Notification permission mili hai ✅");
            tvPermStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
            btnGivePermission.setEnabled(false);
        } else {
            tvPermStatus.setText("Permission nahi mili — button dabao");
            tvPermStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
            btnGivePermission.setEnabled(true);
        }
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        return !TextUtils.isEmpty(flat) && flat.contains(getPackageName());
    }
}
