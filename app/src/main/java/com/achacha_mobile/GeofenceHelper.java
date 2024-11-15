package com.achacha_mobile;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import java.util.ArrayList;
import java.util.List;

public class GeofenceHelper {

    private static final String TAG = "GeofenceHelper";
    private GeofencingClient geofencingClient;
    private Context mContext;
    private boolean isGeofencesBeingAdded = false;  // 중복 실행 방지 플래그

    public GeofenceHelper(Context applicationContext) {
        if (applicationContext == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.mContext = applicationContext.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(mContext);
    }

    // 지오펜스를 등록하는 메소드
    public void addGeofences(MainActivity mainActivity, List<GeofenceData> geofenceDataList) {
        if (isGeofencesBeingAdded) {
            // 이미 지오펜스를 등록 중이면 추가 작업을 막음
            Log.d(TAG, "Geofences are already being added.");
            return;
        }

        // 등록 중 플래그 설정
        isGeofencesBeingAdded = true;

        List<Geofence> geofenceList = new ArrayList<>();
        // 유효한 지오펜스 데이터만 리스트에 추가
        for (GeofenceData data : geofenceDataList) {
            if (isValidGeofenceData(data)) {
                geofenceList.add(new Geofence.Builder()
                        .setRequestId(data.getId()) // 지오펜스의 ID
                        .setCircularRegion(data.getLatitude(), data.getLongitude(), data.getRadius()) // 지오펜스의 위치와 반경
                        .setExpirationDuration(Geofence.NEVER_EXPIRE) // 만료되지 않도록 설정
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT) // 진입 및 퇴장 트리거
                        .build());
            }
        }

        // 지오펜스를 100개씩 나누어서 등록
        int chunkSize = 100; // 한 번에 등록할 최대 개수
        for (int i = 0; i < geofenceList.size(); i += chunkSize) {
            // 100개씩 나누기
            int end = Math.min(i + chunkSize, geofenceList.size());
            List<Geofence> subList = geofenceList.subList(i, end);

            // GeofencingRequest 생성
            GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT)
                    .addGeofences(subList) // 나누어진 지오펜스 리스트 추가
                    .build();

            // PendingIntent 생성 (지오펜스 이벤트를 받을 서비스나 브로드캐스트 리시버)
            PendingIntent pendingIntent = createGeofencePendingIntent();

            // 권한 확인 후 지오펜스 등록
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                isGeofencesBeingAdded = false; // 권한이 없으면 작업을 마친 것으로 간주
                Log.e(TAG, "Location permission is not granted");
                return; // 권한이 없으면 등록하지 않음
            }

            // 지오펜스를 실제로 등록
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Geofences added successfully");
                        isGeofencesBeingAdded = false;  // 등록이 성공적으로 완료되면 플래그를 리셋
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add geofences", e);
                        if (e instanceof ApiException) {
                            ApiException apiException = (ApiException) e;
                            int statusCode = apiException.getStatusCode();
                            // ApiException을 사용하여 오류 코드 처리
                            Log.e(TAG, "Error code: " + statusCode);
                            if (statusCode == GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES) {
                                Log.e(TAG, "Too many geofences! Try reducing the number.");
                            } else if (statusCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                                Log.e(TAG, "Geofence service is not available. Check Play Services.");
                            }
                        }
                        isGeofencesBeingAdded = false;  // 실패 시에도 플래그를 리셋
                    });
        }
    }

    // 유효한 Geofence 데이터를 확인하는 메소드
    private boolean isValidGeofenceData(GeofenceData data) {
        return data.getLatitude() >= -90 && data.getLatitude() <= 90
                && data.getLongitude() >= -180 && data.getLongitude() <= 180
                && data.getRadius() > 0;
    }

    // 지오펜스를 처리할 PendingIntent 생성
    private PendingIntent createGeofencePendingIntent() {
        Intent intent = new Intent(mContext, GeofenceService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }
}
