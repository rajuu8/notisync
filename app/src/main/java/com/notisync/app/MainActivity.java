package com.notisync.app;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.*;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvPermStatus, tvCount, tvConnected;
    private Button btnGivePermission, btnClearAll, btnDisconnect;
    private View statusDot;
    private DatabaseReference dbRef;
    private String userCode;
    private long notifCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Code check karo
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
        statusDot     = findViewById(R.id.status_dot);
        btnGivePermission = findViewById(R.id.btn_give_permission);
        btnClearAll   = findViewById(R.id.btn_clear_all);
        btnDisconnect = findViewById(R.id.btn_disconnect);

        // Connected code dikhao
        tvConnected.setText("Connected Code: " + userCode);

        // Firebase — sirf is user ka data
        dbRef = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("users").child(userCode).child("notifications");

        btnGivePermission.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            Toast.makeText(this, "NotiSync ko allow karo!", Toast.LENGTH_LONG).show();
        });

        btnClearAll.setOnClickListener(v -> {
            dbRef.removeValue();
            notifCount = 0;
            tvCount.setText("0");
            Toast.makeText(this, "Sab clear!", Toast.LENGTH_SHORT).show();
        });

        btnDisconnect.setOnClickListener(v -> {
            getSharedPreferences("notisync", MODE_PRIVATE).edit()
                .remove("user_code").apply();
            startActivity(new Intent(this, ConnectActivity.class));
            finish();
        });

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                notifCount = snapshot.getChildrenCount();
                tvCount.setText(String.valueOf(notifCount));
                tvStatus.setText("Firebase connected — " + notifCount + " notifications");
                statusDot.setBackgroundResource(R.drawable.dot_green);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                tvStatus.setText("Firebase error: " + error.getMessage());
                statusDot.setBackgroundResource(R.drawable.dot_red);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvPermStatus != null) updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        boolean granted = isNotificationListenerEnabled();
        if (granted) {
            tvPermStatus.setText("Permission mili hui hai ✅");
            tvPermStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
            btnGivePermission.setText("Permission di hui hai");
            btnGivePermission.setEnabled(false);
        } else {
            tvPermStatus.setText("Permission nahi mili — button dabao");
            tvPermStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
            btnGivePermission.setText("Permission Do");
            btnGivePermission.setEnabled(true);
        }
    }

    private boolean isNotificationListenerEnabled() {
        String pkgName = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            for (String name : flat.split(":")) {
                if (name.contains(pkgName)) return true;
            }
        }
        return false;
    }
}
