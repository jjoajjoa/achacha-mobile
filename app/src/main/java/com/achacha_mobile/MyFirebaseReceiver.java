package com.achacha_mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class MyFirebaseReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // FCM 데이터에서 액션을 받아 처리
        String action = intent.getStringExtra("action");

        if (action != null) {
            // 액션에 따라 위치 업데이트 시작 또는 중지
            if ("startLocation".equals(action)) {
                startLocationUpdates(context);
            } else if ("stopLocation".equals(action)) {
                stopLocationUpdates(context);
            }
        }
    }

    // 위치 업데이트 시작
    private void startLocationUpdates(Context context) {
        // 이미 서비스가 실행 중인지 체크
        if (!LocationService.isServiceRunning(context)) {
            Intent serviceIntent = new Intent(context, LocationService.class);
            // 포그라운드 서비스로 시작하기 위해 startForegroundService 사용
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }

    // 위치 업데이트 중지
    private void stopLocationUpdates(Context context) {
        Intent serviceIntent = new Intent(context, LocationService.class);
        context.stopService(serviceIntent);  // 서비스 중지
    }
}
