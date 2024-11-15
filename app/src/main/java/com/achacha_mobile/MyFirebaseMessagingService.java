package com.achacha_mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Locale;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FMS";
    private static final String CHANNEL_ID = "1";  // 알림 채널 ID
    private TextToSpeech textToSpeech;  // TTS 객체

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
                startLocationService("start");
            } else if ("stopLocation".equals(action)) {
                // FCM 메시지에서 중지 명령을 받으면 포그라운드 서비스로 실행
                startLocationService("stop");
            }
        }
    }

    private void sendNotification(String title, String messageBody) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

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

    // TTS 함수
    private void readTextWithTTS(String message) {
        if (textToSpeech == null) {
            // TTS 엔진 초기화
            textToSpeech = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int langResult = textToSpeech.setLanguage(Locale.KOREAN); // 한국어 설정
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "한국어 언어 데이터가 부족하거나 지원되지 않음");
                    } else {
                        // UtteranceProgressListener를 통해 음성 출력 후 작업 처리
                        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {
                                Log.d(TAG, "음성 시작: " + utteranceId);
                            }

                            @Override
                            public void onError(String utteranceId) {
                                Log.e(TAG, "음성 오류 발생: " + utteranceId);
                            }

                            @Override
                            public void onDone(String utteranceId) {
                                Log.d(TAG, "음성 완료: " + utteranceId);
                                if ("message_id".equals(utteranceId)) {
                                    releaseTTSResources(); // 음성 출력 완료 후 리소스 해제
                                }
                            }
                        });

                        // 메시지를 TTS로 읽기
                        Log.d(TAG, "TTS 초기화 성공, 메시지 읽기 시작");
                        HashMap<String, String> params = new HashMap<>();
                        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "message_id");
                        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, params);
                    }
                } else {
                    Log.e(TAG, "TTS 초기화 실패");
                }
            });
        } else {
            // 이미 초기화된 TTS 객체 사용
            Log.d(TAG, "TTS 객체 이미 존재, 바로 메시지 읽기");
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "message_id");
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, params);

            // UtteranceProgressListener를 통해 음성 출력 후 작업 처리
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    Log.d(TAG, "음성 시작: " + utteranceId);
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(TAG, "음성 오류 발생: " + utteranceId);
                }

                @Override
                public void onDone(String utteranceId) {
                    Log.d(TAG, "음성 완료: " + utteranceId);
                    if ("message_id".equals(utteranceId)) {
                        releaseTTSResources(); // 음성 출력 완료 후 리소스 해제
                    }
                }
            });
        }
    }


    // LocationService 시작 및 중지 처리
    private void startLocationService(String action) {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.putExtra("action", action);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if ("start".equals(action)) {
                startForegroundService(serviceIntent);
            } else {
                stopService(serviceIntent);
            }
        } else {
            if ("start".equals(action)) {
                startService(serviceIntent);
            } else {
                stopService(serviceIntent);
            }
        }
    }

    private void releaseTTSResources() {
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();  // 음성 출력을 멈추고
                textToSpeech.shutdown(); // 리소스 해제
                Log.d(TAG, "TTS 리소스 정상 해제");
            } catch (Exception e) {
                Log.e(TAG, "TTS 리소스 해제 중 오류 발생: " + e.getMessage());
            } finally {
                textToSpeech = null; // 객체를 null로 설정
            }
        }
    }

}
