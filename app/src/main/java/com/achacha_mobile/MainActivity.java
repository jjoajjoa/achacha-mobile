package com.achacha_mobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.achacha_mobile.network.ApiClient;
import com.achacha_mobile.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 권한 요청
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            // 위치 업데이트 요청
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    double altitude = location.getAltitude();
                    double speed = location.getSpeed();
                    double accuracy = location.getAccuracy();
                    long time = location.getTime();
                    // double direction = location.get //direction은 get함수가 없음
                    // 여기서 GPS 데이터를 서버에 전송
                    sendDataToServer(latitude, longitude, altitude, speed, accuracy, String.valueOf(time));
                }
            });
        }
    }

    private void sendDataToServer(double latitude, double longitude, double altitude, double speed, double accuracy, String time) {
        ApiService apiService = ApiClient.getApiService();
        GpsData gpsData = new GpsData(latitude, longitude, altitude, speed, accuracy, time); // GpsData 객체 생성

        Call<Void> call = apiService.sendGpsData(gpsData);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // 성공적으로 데이터 전송
                    // 예: Toast 메시지 또는 UI 업데이트
                    Toast.makeText(getApplicationContext(), "GPS 데이터 전송 성공", Toast.LENGTH_SHORT).show();
                } else {
                    // 서버가 오류를 반환했을 경우 처리
                    int errorCode = response.code();
                    // 예: 오류 메시지 표시
                    Toast.makeText(getApplicationContext(), "오류 발생: " + errorCode, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // 전송 실패 처리
                // 예: 에러 메시지 표시
                Toast.makeText(getApplicationContext(), "데이터 전송 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
