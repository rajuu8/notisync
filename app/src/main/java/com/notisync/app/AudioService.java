package com.notisync.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.*;
import io.agora.rtc2.*;
import io.agora.rtc2.audio.AudioParams;

public class AudioService extends Service {

    private static final String AGORA_APP_ID = "f5a163996c7b4ff699c03a163c4c66a6";
    private static final String AGORA_TOKEN  = "007eJxTYEjcGPF2oqXX1aebL5w8bbc3K+7Np+3HrKWeRmb2um/V4lJQYEgzTTQ0M7a0NEs2TzJJSzOztEw2MAYJJZskm5klmjnZimU1BDIytC6ZwMrIAIEgPgdDXn5JZnFlXjIDAwBVwCH4";
    private static final String CHANNEL_NAME = "notisync";

    private RtcEngine agoraEngine;
    private DatabaseReference dbRef;
    private String userCode;

    @Override
    public void onCreate() {
        super.onCreate();
        userCode = getSharedPreferences("notisync", MODE_PRIVATE)
                .getString("user_code", null);

        dbRef = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("users").child(userCode).child("audio");

        startForegroundNotif();
        initAgora();
        listenForRequests();
    }

    private void initAgora() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext    = getApplicationContext();
            config.mAppId      = AGORA_APP_ID;
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    dbRef.child("status").setValue("streaming");
                }
                @Override
                public void onLeaveChannel(RtcStats stats) {
                    dbRef.child("status").setValue("idle");
                }
            };
            agoraEngine = RtcEngine.create(config);
            agoraEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            agoraEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            agoraEngine.enableAudio();
            agoraEngine.muteLocalAudioStream(true); // Default mute
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenForRequests() {
        dbRef.child("request").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String req = snapshot.getValue(String.class);
                if ("start".equals(req)) {
                    startStreaming();
                } else if ("stop".equals(req)) {
                    stopStreaming();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void startStreaming() {
        agoraEngine.muteLocalAudioStream(false);
        agoraEngine.joinChannel(AGORA_TOKEN, CHANNEL_NAME, 0, null);
        dbRef.child("status").setValue("streaming");
        // Notification update
        showListeningNotif();
    }

    private void stopStreaming() {
        agoraEngine.muteLocalAudioStream(true);
        agoraEngine.leaveChannel();
        dbRef.child("status").setValue("idle");
        dbRef.child("request").setValue("idle");
        startForegroundNotif();
    }

    private void startForegroundNotif() {
        createChannel();
        android.app.Notification notif = new NotificationCompat.Builder(this, "audio_ch")
            .setContentTitle("NotiSync")
            .setContentText("Audio monitoring ready")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
        startForeground(1, notif);
    }

    private void showListeningNotif() {
        createChannel();
        android.app.Notification notif = new NotificationCompat.Builder(this, "audio_ch")
            .setContentTitle("NotiSync — Streaming 🎙️")
            .setContentText("Mic on hai — tap to open")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1, notif);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                "audio_ch", "Audio Service", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (agoraEngine != null) {
            agoraEngine.leaveChannel();
            RtcEngine.destroy();
        }
    }
}
