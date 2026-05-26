package com.notisync.app;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import com.google.firebase.database.*;
import java.util.HashMap;
import java.util.Map;

public class NotificationListenerService
        extends android.service.notification.NotificationListenerService {

    private DatabaseReference dbRef;
    private String userCode;

    private static final String[] IGNORE_PACKAGES = {
        "android", "com.android.systemui", "com.android.phone",
        "com.google.android.gms", "com.android.launcher",
        "com.miui.home", "com.samsung.android.app.spage",
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // User code lo
        userCode = getSharedPreferences("notisync", MODE_PRIVATE)
                .getString("user_code", null);

        if (userCode != null) {
            dbRef = FirebaseDatabase.getInstance(
                "https://notisync-82fce-default-rtdb.firebaseio.com"
            ).getReference("users").child(userCode).child("notifications");
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || dbRef == null || userCode == null) return;

        String packageName = sbn.getPackageName();
        for (String ignore : IGNORE_PACKAGES) {
            if (packageName.startsWith(ignore)) return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        CharSequence titleSeq = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textSeq  = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigText  = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

        String title = titleSeq != null ? titleSeq.toString().trim() : "";
        String msg   = bigText  != null ? bigText.toString().trim()
                     : textSeq  != null ? textSeq.toString().trim() : "";

        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(msg)) return;

        String appName = getAppName(packageName);
        String type    = determineType(packageName, title, msg);

        Map<String, Object> data = new HashMap<>();
        data.put("app",     appName);
        data.put("package", packageName);
        data.put("type",    type);
        data.put("title",   TextUtils.isEmpty(title) ? "(No title)" : title);
        data.put("msg",     msg);
        data.put("time",    System.currentTimeMillis());
        data.put("read",    false);

        dbRef.push().setValue(data);
    }

    private String getAppName(String packageName) {
        try {
            android.content.pm.ApplicationInfo info =
                getPackageManager().getApplicationInfo(packageName, 0);
            return getPackageManager().getApplicationLabel(info).toString();
        } catch (Exception e) {
            String[] parts = packageName.split("\\.");
            if (parts.length > 0) {
                String name = parts[parts.length - 1];
                return name.substring(0, 1).toUpperCase() + name.substring(1);
            }
            return packageName;
        }
    }

    private String determineType(String pkg, String title, String msg) {
        if (pkg.contains("whatsapp") || pkg.contains("telegram") ||
            pkg.contains("messenger") || pkg.contains("signal") ||
            pkg.contains("instagram") || pkg.contains("twitter") ||
            pkg.contains("snapchat")) return "message";
        if (pkg.contains("gmail") || pkg.contains("email") ||
            pkg.contains("mail") || pkg.contains("outlook")) return "alert";
        if (pkg.contains("calendar") || pkg.contains("clock") ||
            pkg.contains("alarm")) return "reminder";
        if (pkg.contains("playstore") || pkg.contains("market") ||
            pkg.contains("update")) return "update";
        String combined = (title + " " + msg).toLowerCase();
        if (combined.contains("offer") || combined.contains("sale") ||
            combined.contains("discount")) return "promo";
        return "alert";
    }
}
