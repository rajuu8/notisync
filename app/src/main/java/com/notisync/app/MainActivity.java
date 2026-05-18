package com.notisync.app;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvPermStatus, tvCount;
    private Button btnGivePermission, btnClearAll;
    private View statusDot;
    private DatabaseReference dbRef;
    private long notifCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus      = findViewById(R.id.tv_status);
        tvPermStatus  = findViewById(R.id.tv_perm_status);
        tvCount       = findViewById(R.id.tv_count);
        statusDot     = findViewById(R.id.status_dot);
        btnGivePermission = findViewById(R.id.btn_give_permission);
        btnClearAll   = findViewById(R.id.btn_clear_all);

        // Firebase connection
        dbRef = FirebaseDatabase.getInstance(
            "https://earning-app-32911-default-rtdb.firebaseio.com"
        ).getReference("notifications");

        btnGivePermission.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            Toast.makeText(this, "NotiSync ko allow karo!", Toast.LENGTH_LONG).show();
        });

        btnClearAll.setOnClickListener(v -> {
            dbRef.removeValue();
            Toast.makeText(this, "Sab notifications delete ho gaye!", Toast.LENGTH_SHORT).show();
            notifCount = 0;
            tvCount.setText("0");
        });

        // Firebase se count fetch karo
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                notifCount = snapshot.getChildrenCount();
                tvCount.setText(String.valueOf(notifCount));
                tvStatus.setText("Firebase connected — " + notifCount + " notifications synced");
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
        updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        boolean granted = isNotificationListenerEnabled();
        if (granted) {
            tvPermStatus.setText("Permission mili hui hai — notifications sync ho rahe hain");
            tvPermStatus.setTextColor(ContextCompat.getColor(this, R.color.green));
            btnGivePermission.setText("Permission di hui hai");
            btnGivePermission.setEnabled(false);
        } else {
            tvPermStatus.setText("Permission nahi mili — neeche button dabao");
            tvPermStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
            btnGivePermission.setText("Permission Do");
            btnGivePermission.setEnabled(true);
        }
    }

    private boolean isNotificationListenerEnabled() {
        String pkgName = getPackageName();
        String flat = Settings.Secure.getString(
            getContentResolver(),
            "enabled_notification_listeners"
        );
        if (!TextUtils.isEmpty(flat)) {
            String[] names = flat.split(":");
            for (String name : names) {
                if (name.contains(pkgName)) return true;
            }
        }
        return false;
    }
}
