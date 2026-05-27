package com.notisync.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.*;
import io.agora.rtc2.*;

public class AudioService extends Service {

    private static final String AGORA_APP_ID = "f5a163996c7b4ff699c03a163c4c66a6";
    private static final String AGORA_TOKEN  = "007eJxTYEjcGPF2oqXX1aebL5w8bbc3K+7Np+3HrKWeRmb2um/V4lJQYEgzTTQ0M7a0NEs2TzJJSzOztEw2MAYJJZskm5klmjnZimU1BDIytC6ZwMrIAIEgPgdDXn5JZnFlXjIDAwBVwCH4";
    private static final String CHANNEL      = "notisync_audio";

    private RtcEngine engine;
    private DatabaseReference audioRef;
    private String userCode;
    private boolean isStreaming = false;

    @Override
    public void onCreate() {
        super.onCreate();
        userCode = getSharedPreferences("notisync", MODE_PRIVATE)
                .getString("user_code", null);
        if (userCode == null) return;

        audioRef = FirebaseDatabase.getInstance(
            "https://notisync-82fce-default-rtdb.firebaseio.com"
        ).getReference("users").child(userCode).child("audio");

        startForegroundNotif("NotiSync Audio Ready", "Waiting for listener...");
        initAgora();
        listenRequests();
    }

    private void initAgora() {
        try {
            RtcEngineConfig cfg = new RtcEngineConfig();
            cfg.mContext     = getApplicationContext();
            cfg.mAppId       = AGORA_APP_ID;
            cfg.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String ch, int uid, int e) {
                    isStreaming = true;
                    audioRef.child("status").setValue("streaming");
                    startForegroundNotif("🎙️ Streaming", "Phone 2 sun raha hai");
                }
                @Override
                public void onLeaveChannel(RtcStats s) {
                    isStreaming = false;
                    audioRef.child("status").setValue("idle");
                    startForegroundNotif("NotiSync Audio Ready", "Waiting for listener...");
                }
                @Override
                public void onError(int err) {
                    audioRef.child("status").setValue("error:" + err);
                }
            };
            engine = RtcEngine.create(cfg);
            // IMPORTANT: Broadcaster mode - mic on
            engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            engine.enableAudio();
            engine.setAudioProfile(
                Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY,
                Constants.AUDIO_SCENARIO_CHATROOM
            );
            engine.muteLocalAudioStream(false);
            engine.adjustRecordingSignalVolume(400); // Volume boost
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenRequests() {
        audioRef.child("request").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                String req = snap.getValue(String.class);
                if ("start".equals(req) && !isStreaming) {
                    engine.joinChannel(AGORA_TOKEN, CHANNEL, 0, null);
                } else if ("stop".equals(req) && isStreaming) {
                    engine.leaveChannel();
                    audioRef.child("request").setValue("idle");
                }
            }
            @Override public void onCancelled(DatabaseError e) {}
        });
    }

    private void startForegroundNotif(String title, String msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                "audio_ch", "Audio", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .createNotificationChannel(ch);
        }
        android.app.Notification n = new NotificationCompat.Builder(this, "audio_ch")
            .setContentTitle(title)
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build();
        startForeground(1, n);
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (engine != null) {
            engine.leaveChannel();
            RtcEngine.destroy();
        }
    }
}
