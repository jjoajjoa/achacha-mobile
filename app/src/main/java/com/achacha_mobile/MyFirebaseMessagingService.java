package com.achacha_mobile;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FMS";

    public MyFirebaseMessagingService() {

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // 메시지 수신 시 실행할 로직
        if (remoteMessage.getData().size() > 0) {
            // 데이터 메시지 처리
            String action = remoteMessage.getData().get("action");
//            handleAction(action);
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
}
