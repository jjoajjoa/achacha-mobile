package com.achacha_mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceService extends android.app.Service {
    private static final String TAG = "GeofenceService";
    private static final String CHANNEL_ID = "GeofenceChannel";
    private static final int ENTER_NOTIFICATION_ID = 1;
    private static final int EXIT_NOTIFICATION_ID = 2;

    public GeofenceService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 포그라운드 서비스 시작
        startForegroundService();

        // 지오펜스 이벤트 처리
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing event error: " + geofencingEvent.getErrorCode());
            return START_STICKY;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // 지오펜스 진입 혹은 이탈 처리
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            handleEnterEvent(geofencingEvent);
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            handleExitEvent(geofencingEvent);
        }

        return START_STICKY; // 서비스가 종료되지 않도록 유지
    }

    // 지오펜스 진입 처리
    private void handleEnterEvent(GeofencingEvent event) {
        for (Geofence geofence : event.getTriggeringGeofences()) {
            String geofenceId = geofence.getRequestId();
            Log.d(TAG, "지역 도착: " + geofenceId);
            // 진입 알림
            showGeofenceNotification("지역 도착", "운행 종료 감지됨", ENTER_NOTIFICATION_ID);
        }
    }

    // 지오펜스 이탈 처리
    private void handleExitEvent(GeofencingEvent event) {
        for (Geofence geofence : event.getTriggeringGeofences()) {
            String geofenceId = geofence.getRequestId();
            Log.d(TAG, "지역 출발: " + geofenceId);
            // 이탈 알림
            showGeofenceNotification("지역 출발", "운행 시작 감지됨", EXIT_NOTIFICATION_ID);
        }
    }

    // 포그라운드 서비스를 시작하는 메소드
    private void startForegroundService() {
        // 포그라운드 서비스 채널 생성 (API 26 이상에서 필요)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Geofence Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Channel for geofence service notifications");
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        // 포그라운드 서비스에 표시할 알림 설정
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("지역 감지 중")
                .setContentText("운행 시작 감지 중")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();

        // 포그라운드 서비스로 설정
        startForeground(1, notification);
    }

    // 알림 표시 메소드 (진입/이탈에 따라 다르게 표시)
    private void showGeofenceNotification(String title, String text, int notificationId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification); // 고유 ID로 알림 표시
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 서비스가 종료될 때 필요한 작업을 추가할 수 있습니다.
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null; // 바인딩하지 않으므로 null 반환
    }
}
