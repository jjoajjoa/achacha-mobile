package com.achacha_mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import androidx.annotation.NonNull;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String BASE_URL = "http://175.197.201.115:9000/";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1; // 위치 권한 성공 했다는 코드 (지오팬스)
    private GeofenceHelper geofenceHelper;

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

        // 지오펜스 위치 설정 ----------------------------------------------------------
        geofenceHelper = new GeofenceHelper(this);
        List<GeofenceData> geofenceDataList = new ArrayList<>();
        // ( 고유 Id , 경도, 위도 , 거리 ( 100f = 100미터 ) )  위치 오류가 20미터씩 나기 떄문에 100미터는 설정 해야 된다고 권장
        geofenceDataList.add(new GeofenceData("1", 37.516123, 127.035089, 10f)); // 공간정보 아카데미 반경 10미터
        geofenceDataList.add(new GeofenceData("2", 37.514544, 127.032107, 80f)); // 학동역

        // GeofenceHelper로 지오펜스 추가
        geofenceHelper.addGeofences(this, geofenceDataList);

        // GeofenceService와 LocationService 시작
        Intent locationServiceIntent = new Intent(this, LocationService.class);
        startService(locationServiceIntent);

        //--------------------------------------------------------------------

        SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null); // 기본값 설정

        //웹뷰 설정
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // WebView의 설정
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);  // JavaScript 활성화
        webSettings.setDomStorageEnabled(true);  // 로컬 스토리지 활성화 (웹 앱에서 로컬 스토리지 사용 시 필요)
        webSettings.setAllowFileAccess(true);  // 파일 접근 허용
        webSettings.setAllowContentAccess(true);  // 콘텐츠 접근 허용
        webSettings.setUseWideViewPort(true);  // 웹 페이지에 맞는 화면 크기 설정
        webSettings.setLoadWithOverviewMode(true);  // 페이지 로딩 방식 설정
        webSettings.setSupportZoom(true);  // 줌 설정 허용 (모바일에서 유용)
        webSettings.setBuiltInZoomControls(true);  // 기본 줌 컨트롤 활성화
        webSettings.setDisplayZoomControls(false);  // 줌 컨트롤 UI를 숨기기
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);  // 캐시 모드 설정

        if (userId == null) {
            webView.loadUrl("http://175.197.201.115:8080/applogin");
        } else {
            Log.d("userId",userId);
            webView.loadUrl("http://175.197.201.115:8080/apphome");
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 위치 요청 설정
        locationRequest = new LocationRequest.Builder(5000)
                .setMinUpdateIntervalMillis(2000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        Button startButton = findViewById(R.id.start_button);
        Button stopButton = findViewById(R.id.stop_button);

        // 서비스 시작 버튼 클릭 시 서비스 시작
        startButton.setOnClickListener(v -> startLocationUpdates());

        // 서비스 종료 버튼 클릭 시 서비스 종료
        stopButton.setOnClickListener(v -> stopLocationUpdates());

    }

    public void startLocationUpdates() {
        // LocationService를 시작하여 위치 업데이트를 백그라운드에서 처리
        if (!LocationService.isServiceRunning(this)) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            startService(serviceIntent);  // 서비스 시작
            //Toast.makeText(this, "위치 업데이트 시작", Toast.LENGTH_SHORT).show();
        } else {
           // Toast.makeText(this, "서비스가 이미 실행 중입니다.", Toast.LENGTH_SHORT).show();
        }
       // fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        Toast.makeText(this, "위치 업데이트 시작", Toast.LENGTH_SHORT).show();
    }

    public void stopLocationUpdates() {
        // LocationService 종료
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);  // 서비스 종료
        Toast.makeText(this, "위치 업데이트 중지", Toast.LENGTH_SHORT).show();
    }

    void sendIdandToken(String userId, String token) {
        try {
            // URL에 쿼리 매개변수 추가
            String urlString = String.format("%s/noti/sendToken?userId=%s&token=%s",
                    BASE_URL,
                    URLEncoder.encode(userId, "UTF-8"),
                    URLEncoder.encode(token, "UTF-8"));

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(); // 연결 생성
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json"); // 헤더 설정

            // 응답 코드 확인
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("전송 성공");
            } else {
                System.out.println("전송 실패: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        sendIdandToken(token, userId);

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
