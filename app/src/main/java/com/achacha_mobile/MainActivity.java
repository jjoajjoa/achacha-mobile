package com.achacha_mobile;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String BASE_URL = "http://172.168.10.88:9000/";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    // 웹뷰
    private WebView webView;

    // 외부 알림
    NotificationManager manager;
    private static String CHANNEL_ID3 = "channel3";
    private static String CHANNEL_NAME3 = "Channel3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
        //웹뷰 설정
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://172.168.10.88:8080/applogin");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 위치 요청 설정
        locationRequest = new LocationRequest.Builder(5000)
                .setMinUpdateIntervalMillis(2000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        // 위치 권한 요청
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        Button startButton = findViewById(R.id.start_button);
        Button stopButton = findViewById(R.id.stop_button);

        // 서비스 시작 버튼 클릭 시 서비스 시작
        startButton.setOnClickListener(v -> startLocationUpdates());

        // 서비스 종료 버튼 클릭 시 서비스 종료
        stopButton.setOnClickListener(v -> stopLocationUpdates());

        // 위치 업데이트 콜백 설정
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "New location: " + location.toString()); // 새로운 위치 로그
                   // sendLocationToServer(location); -- 포그라운드 에서 실행 함 - 없어도 됨
                }
            }
        };
        // 토큰 가져오기
        fetchFCMToken();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        Toast.makeText(this, "위치 업데이트 시작", Toast.LENGTH_SHORT).show();

    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Toast.makeText(this, "위치 업데이트 중지", Toast.LENGTH_SHORT).show();

        showEmergencyNoti();
    }

    interface ApiService {
        @POST("/api/location/update")
        Call<Void> updateLocation(@Body GpsData location);
    }

    void fetchFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();
                        // 이 토큰으로 기기 인식 시키고 알람보낼 때 확인 할 수 있게 함

                        // Log and toast
                        String msg = "FCM Token: " + token;
                        Log.d(TAG, msg);

                        // 토큰을 서버에 전송
                        sendTokenToServer(token);
                    }
                });
    }

    //데이터베이스로 보내는 함수
    private void sendTokenToServer(String token) {
        // 토큰과 타임스탬프를 담을 맵 생성
        Map<String, Object> deviceToken = new HashMap<>();
        deviceToken.put("token", token);
        deviceToken.put("timestamp", FieldValue.serverTimestamp());

        // 사용자 아이디 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", "defaultUser"); // 기본값 설정

        // Firestore에 데이터 저장
        FirebaseFirestore.getInstance().collection("fcmTokens")
                .document(userId) // 여기에 저장됨 - 나중에 로그인 하면 그 아이디로 저장
                .set(deviceToken)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully saved to Firestore."))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving token to Firestore: " + e.getMessage()));
    }


    // 졸음 꺠우기 알림
    public void showEmergencyNoti() {
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID3,
                    CHANNEL_NAME3,
                    NotificationManager.IMPORTANCE_HIGH // 강제 알림
            );
            channel.setDescription("긴급 알림");
            channel.enableLights(true);
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(this, CHANNEL_ID3);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long[] vibrationPattern = {0, 500, 1000}; // 진동 패턴 설정
        builder.setContentTitle("졸음 운전 경고!")
                .setContentText("운전 중 졸음이 감지되었습니다.")
                .setSmallIcon(android.R.drawable.ic_menu_view) // 임시 아이콘 -- 긴금으로 바꿔야함
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(vibrationPattern) // 진동 추가
                .setSound(Settings.System.DEFAULT_ALARM_ALERT_URI); // 기본 알람 소리 설정

        Notification noti = builder.build();
        manager.notify(3, noti);
    }
    
}
