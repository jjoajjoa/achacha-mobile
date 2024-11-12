package com.achacha_mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Locale;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FMS";
    String CHANNEL_ID = "1";
    private TextToSpeech textToSpeech;

    public MyFirebaseMessagingService() {

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            String action = remoteMessage.getData().get("action");

            // FCM 메시지에서 알림의 텍스트(body) 추출
            String notificationMessage = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "새로운 메시지 도착";

            // 받은 메시지를 TTS로 읽기
            readTextWithTTS(notificationMessage);

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
                    stopForeground(serviceIntent.hasFileDescriptors());
                } else {
                    stopService(serviceIntent);
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

    // tts 함수
    private void readTextWithTTS(String message) {
        if (textToSpeech == null) {
            // TTS 엔진 초기화
            textToSpeech = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int langResult = textToSpeech.setLanguage(Locale.KOREAN); // 한국어 설정
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "지원하지 않는 언어");
                    } else {
                        // 받은 메시지를 TTS로 읽기
                        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
                        // TTS 실행 중에 포그라운드 서비스 시작
                        //startForegroundService(new Intent(this, ForegroundService.class));
                    }
                } else {
                    // 이미 TTS 객체가 있으면 바로 음성 출력
                    textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
                    Log.e(TAG, "TTS 초기화 실패");
                }
            });
        } else {
            // 이미 TTS 객체가 있으면 바로 음성 출력
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown(); // 리소스 해제
        }
        super.onDestroy();
    }

}