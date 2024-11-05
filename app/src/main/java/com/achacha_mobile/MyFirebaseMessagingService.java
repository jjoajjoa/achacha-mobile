package com.achacha_mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FMS";

    public MyFirebaseMessagingService() {

    }

    private static final String CHANNEL_ID = "my_channel_id";
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // 데이터 메시지를 수신한 경우
        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            sendNotification(title, body);
        }
    }

    private void sendNotification(String title, String messageBody) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 알림 채널 설정 (Android 8.0 이상)
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

