package com.achacha_mobile;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceService extends Service {
    private static final String TAG = "GeofenceService";
    private LocationService locationService;

    @Override
    public void onCreate() {
        super.onCreate();
        locationService = new LocationService();  // LocationService 인스턴스 생성
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: " + geofencingEvent.getErrorCode());
            return START_NOT_STICKY;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            handleEnterEvent();
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            handleExitEvent();
        }

        return START_NOT_STICKY;
    }

    // 진입 시 호출
    private void handleEnterEvent() {
        Log.d(TAG, "운행 종료");
        locationService.startTransmitting();  // 로케이션 전송 종료
    }

    // 이탈 시 호출
    private void handleExitEvent() {
        Log.d(TAG, "운행 시작");
        locationService.stopTransmitting();  // 로케이션 전송 시작
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 바인딩하지 않음
    }
}
