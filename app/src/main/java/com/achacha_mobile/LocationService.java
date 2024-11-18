package com.achacha_mobile;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationService extends Service {
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private PowerManager.WakeLock wakeLock; // Wake Lock
    private boolean isTransmitting = false; // 위치 전송 여부 변수
    private boolean isDriving = false; // 운행 상태 여부 변수

    private LocationCallback locationCallback; // 위치 업데이트를 받을 콜백 객체

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel(); // 알림 채널 생성
        acquireWakeLock(); // Wake Lock 획득

        // 위치 콜백 객체 초기화
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (isDriving && locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    sendLocationToServer(location); // 위치 데이터를 서버로 전송
                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                // 위치 서비스 사용 가능 여부 처리
                if (!locationAvailability.isLocationAvailable()) {
                    Log.d("LocationService", "위치 서비스 불가");
                }
            }
        };
    }

    // Wake Lock 획득
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::LocationServiceWakelock");
        if (wakeLock != null) {
            wakeLock.acquire(); // Wake Lock 획득
        }
    }

    private LocationRequest createLocationRequest() {
        return new LocationRequest.Builder(10000)
                .setMinUpdateIntervalMillis(2000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra("action");

        if ("start".equals(action)) {
            startForeground(1, createNotification()); // 알림 생성
            isDriving = true; // 운행 시작
            startLocationUpdates(); // 위치 업데이트 시작
        } else if ("stop".equals(action)) {
            stopForeground(true); // 알림 제거
            stopSelf(); // 서비스 종료
            isDriving = false; // 운행 중지
            isTransmitting = false; // 위치 전송 중지
        }

        return START_NOT_STICKY;
    }

    private void startLocationUpdates() {
        locationRequest = createLocationRequest();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // 권한이 없으면 위치 업데이트 요청하지 않음
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release(); // Wake Lock 해제
        }
        fusedLocationClient.removeLocationUpdates(locationCallback);  // 위치 업데이트 중지
        stopLocationUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 위치 업데이트 중지
    public void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);

        stopForeground(true);  // 포그라운드 서비스 종료
        stopSelf();  // 서비스 종료
    }

    // 알림 생성 메서드
    private Notification createNotification() {
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Monitoring")
                .setContentText("Monitoring your GPS...")
                .setPriority(Notification.PRIORITY_HIGH); // 중요도 설정
        return builder.build();
    }

    // 알림 채널 생성
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "위치 서비스 채널",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void sendLocationToServer(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double altitude = location.getAltitude();
        double speed = location.getSpeed();
        double accuracy = location.getAccuracy();
        long millisecondTime = location.getTime();
        String time = formatDate(millisecondTime);

        // 사용자 아이디 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
        String employeeId = sharedPreferences.getString("userId", null); // 기본값 설정
        Log.e("userID", employeeId);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://175.197.201.115:9000/") // 서버 주소 설정
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MainActivity.ApiService apiService = retrofit.create(MainActivity.ApiService.class);
        GpsData gpsData = new GpsData(latitude, longitude, altitude, speed, accuracy, time, employeeId);

        Call<Void> call = apiService.updateLocation(gpsData);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "GPS 데이터 전송 성공", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "오류 발생: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("Retrofit Error", "데이터 전송 실패: " + t.getMessage());
                Toast.makeText(getApplicationContext(), "데이터 전송 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDate(long timeInMillis) {
        Date date = new Date(timeInMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        return sdf.format(date);
    }

    interface ApiService {
        @POST("/api/location/update")
        Call<Void> updateLocation(@Body GpsData location);
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    // 위치 전송 시작 메서드
    public void startTransmitting() {
        isDriving = true; // 운행 시작
        isTransmitting = true; // 위치 전송 시작
        startLocationUpdates(); // 위치 업데이트 시작
        Log.d("LocationService", "위치 전송 시작");
    }

    // 위치 전송 중지 메서드
    public void stopTransmitting() {
        isDriving = false; // 운행 중지
        isTransmitting = false; // 위치 전송 중지
        stopLocationUpdates();  // 위치 업데이트 중지
        Log.d("LocationService", "위치 전송 중지");
    }
}
