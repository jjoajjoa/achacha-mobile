package com.achacha_mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FMS";
    String CHANNEL_ID = "1";
    public MyFirebaseMessagingService() {

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");

            if ("startLocation".equals(action)) {
                // FCM 메시지에서 시작 명령을 받으면 포그라운드 서비스로 실행
                Intent serviceIntent = new Intent(this, LocationService.class);
                serviceIntent.putExtra("action", "start");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else if ("stopLocation".equals(action)) {
                // FCM 메시지에서 중지 명령을 받으면 포그라운드 서비스로 실행
                Intent serviceIntent = new Intent(this, LocationService.class);
                serviceIntent.putExtra("action", "stop");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }
        }
    }

    private void sendNotification(String title, String messageBody) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                //.setSmallIcon(R.drawable.ic_notification)  // 알림 아이콘
                ;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // 알림 표시
        notificationManager.notify(0, notificationBuilder.build());
    }
}

   /* private void handleAction(String action) {
        if ("start_noti".equals(action)) {
            // 알림 시작 함수 호출
            ((MainActivity) getApplicationContext()).showStartNoti();
        } else if ("end_noti".equals(action)) {
            // 알림 종료 함수 호출
            ((MainActivity) getApplicationContext()).showEndNoti();
        } else if ("emergency_noti".equals(action)) {
            // 긴급 알림 함수 호출
            ((MainActivity) getApplicationContext()).showEmergencyNoti();
        }
    }*/

