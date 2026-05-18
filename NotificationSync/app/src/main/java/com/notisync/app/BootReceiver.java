package com.notisync.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Service Android OS khud restart karta hai NotificationListenerService ke liye
            // Yahan koi extra kaam nahi
        }
    }
}
