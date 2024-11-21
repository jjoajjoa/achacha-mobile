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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.achacha_mobile.utility.PermissionUtility;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.mediapipe.framework.MediaPipeException;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mlkit.vision.face.FaceLandmark;

import org.tensorflow.lite.support.image.ImageProperties;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;


// Retrofit: GPS 데이터를 서버로 전송하는데 사용
// FusedLocationProviderClient: 위치 서비스를 사용하여 GPS 정보를 가져옴
// NotificationManager: 알림 처리
// CameraX: 카메라 기능을 쉽게 사용할 수 있게 해주는 라이브러리
// MediaPipe: 얼굴 랜드마크 추출을 위한 모델을 사용
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String BASE_URL = "http://172.168.30.184:9000/";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    // 웹뷰
    private WebView webView;

    // 외부 알림
    NotificationManager manager;
    private static String CHANNEL_ID = "channel";
    private static String CHANNEL_NAME = "Channel";

    // 권한 요청 코드
    private static final int PERMISSION_REQUEST_CODE = 100;

    // 카메라 권한 요청 코드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    // MediaPipe - 객체인식
    private FaceLandmarker faceLandmarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null); // 기본값 설정

        //웹뷰 설정
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient()); // 링크 클릭 시 새 브라우저 열리지 않도록 설정
        webView.addJavascriptInterface(new WebAppInterface(this), "Android"); // 웹앱 인터페이스 안에 있는 함수를 실행 시킬 수 있음 - 일단 토스트로

        // 웹 설정
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // JavaScript 사용 가능하게 설정
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
            webView.loadUrl("http://172.168.30.184:8080/applogin");
        } else {
            Log.d("userId",userId);
            webView.loadUrl("http://172.168.30.184:8080/apphome");
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 위치 요청 설정
        locationRequest = new LocationRequest.Builder(5000)
                .setMinUpdateIntervalMillis(2000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        // 필요한 권한 배열 정의
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION};

        // 권한 요청
        PermissionUtility.requestPermissions(this, permissions);

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

    } //onCreate

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1023) { // 권한 요청 코드 확인
            if (PermissionUtility.arePermissionsGranted(this, permissions)) {
                // 모든 권한이 허용된 경우
                onPermissionsGranted();
            } else {
                // 권한이 거부된 경우 처리
                handlePermissionDenied(permissions);
            }
        }
    }

    // 권한 허용 시 처리
    private void onPermissionsGranted() {
        // 권한이 모두 허용되었을 때의 로직 추가
        loadModel();
        if (faceLandmarker != null) {
            startCamera();
        } else {
            Log.e(TAG, "FaceLandmarker 초기화 실패. 카메라를 시작할 수 없습니다.");
            Toast.makeText(this, "모델 로드 실패! 앱을 다시 실행해주세요.", Toast.LENGTH_LONG).show();
        }
        startLocationUpdates(); // 위치 업데이트 시작
    }

    // 권한 거부 시 처리
    private void handlePermissionDenied(String[] deniedPermissions) {
        StringBuilder deniedMessage = new StringBuilder("다음 권한이 필요합니다:\n");
        for (String permission : deniedPermissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                // "다시 묻지 않음" 옵션을 선택한 경우
                deniedMessage.append(permission).append(" (설정에서 수동으로 허용해야 합니다)\n");
            } else {
                deniedMessage.append(permission).append("\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("권한 필요")
                .setMessage(deniedMessage.toString())
                .setPositiveButton("설정으로 이동", (dialog, which) -> openAppSettings())
                .setNegativeButton("취소", null)
                .show();
    }

    // 설정 화면으로 이동
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }


    public void startLocationUpdates() {
        // LocationService를 시작하여 위치 업데이트를 백그라운드에서 처리
        if (!LocationService.isServiceRunning(this)) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            startService(serviceIntent);  // 서비스 시작
            Toast.makeText(this, "위치 업데이트 시작", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "서비스가 이미 실행 중입니다.", Toast.LENGTH_SHORT).show();
        }
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
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

    private void sendLocationToServer(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double altitude = location.getAltitude();
        double speed = location.getSpeed();
        double accuracy = location.getAccuracy();
        long millisecondTime = location.getTime();
        String time = formatDate(millisecondTime);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SharedPreferences sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null); // 기본값 설정

        ApiService apiService = retrofit.create(ApiService.class);
        GpsData gpsData = new GpsData(latitude, longitude, altitude, speed, accuracy, time, userId);

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

    public String formatDate(long timeInMillis) {
        Date date = new Date(timeInMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        return sdf.format(date);
    }



    // 알림 함수
    public void showStartNoti() {
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 채널 설정
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            // 추가 설정 가능
            channel.setDescription("운행 관련 알림");
            manager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
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

        builder.setContentTitle("GPS 시작")
                .setContentText("운행이 시작되었습니다")
                .setSmallIcon(android.R.drawable.ic_menu_view) // 임시 아이콘
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        Notification noti = builder.build();
        manager.notify(1, noti);

        // LocationService 시작
        Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);

    } //showStartNoti

    // FaceLandmarker 객체를 생성하고, face_landmark.tflite 모델 파일을 로드
    private void loadModel() {
        try {
            faceLandmarker = FaceLandmarker.createFromFile(this, "face_landmark.tflite");
            if (faceLandmarker == null) {
                Log.e(TAG, "FaceLandmarker 초기화 실패");
            } else {
                Log.d(TAG, "FaceLandmarker 초기화 성공.");
            }
        } catch (MediaPipeException e) {  // MediaPipe 관련 예외 처리
            Log.e(TAG, "MediaPipe error loading model: " + e.getMessage());
        } catch (Exception e) {  // 다른 예외 처리
            Log.e(TAG, "Error loading model: " + e.getMessage());
        }
    }

    // 카메라를 초기화하고, 카메라 프리뷰와 이미지 분석을 설정
    // ProcessCameraProvider를 사용하여 카메라를 바인딩
    // ImageAnalysis를 사용하여 카메라에서 캡처된 이미지를 분석하고, 얼굴 랜드마크를 감지
    // imageToBitmap() 메서드에서 ImageProxy를 Bitmap으로 변환하고, 이를 MediaPipe에서 처리할 수 있도록 MPImage로 변환
    private void startCamera() {
        Log.d(TAG, "카메라 시작 시도");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                PreviewView previewView = findViewById(R.id.preview_view); // PreviewView 연결
                Preview preview = new Preview.Builder()
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    //Log.d(TAG, "이미지 분석 시작");

                    if (faceLandmarker != null) {
                        Bitmap bitmap = imageToBitmap(image); // 이미지 변환
                        if (bitmap != null) {
                            Log.d(TAG, "이미지 변환 완료");

                            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
                            FaceLandmarkerResult result = faceLandmarker.detect(mpImage); // 얼굴 랜드마크 감지

                            if (result != null && !result.faceLandmarks().isEmpty()) {
                                Log.d(TAG, "얼굴 랜드마크 감지 완료");
                                analyzeFaceLandmarks(result); // 랜드마크 분석
                            } else {
                                Log.d(TAG, "얼굴 랜드마크 감지 실패 또는 얼굴이 없음");
                            }
                        } else {
                            Log.d(TAG, "이미지 변환 실패");
                        }
                    } else {
                        Log.e(TAG, "메롱");
                    }

                    image.close();
                });

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        Log.d(TAG, "이미지 변환 중...");
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();

        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (bitmap != null) {
            Log.d(TAG, "Bitmap 변환 성공");
        } else {
            Log.d(TAG, "Bitmap 변환 실패");
        }
        return bitmap;
    }

    private void analyzeFaceLandmarks(FaceLandmarkerResult result) {
        if (result == null || result.faceLandmarks().isEmpty()) {
            Log.d(TAG, "얼굴이 감지되지 않았습니다.");
            return;
        }

        Log.d(TAG, "얼굴 감지됨, 랜드마크 분석 시작");
        List<NormalizedLandmark> faceLandmarks = result.faceLandmarks().get(0);

        // 왼쪽 눈과 오른쪽 눈의 랜드마크 추출
        List<NormalizedLandmark> leftEyeLandmarks = getLeftEyeLandmarks(faceLandmarks);
        List<NormalizedLandmark> rightEyeLandmarks = getRightEyeLandmarks(faceLandmarks);

        // 눈 감김 상태 확인
        boolean isLeftEyeClosed = isEyeClosed(leftEyeLandmarks);
        Log.d(TAG, "왼쪽 눈 상태: " + (isLeftEyeClosed ? "감김" : "열림"));

        boolean isRightEyeClosed = isEyeClosed(rightEyeLandmarks);
        Log.d(TAG, "오른쪽 눈 상태: " + (isRightEyeClosed ? "감김" : "열림"));

        // 두 눈이 모두 감겼다면, 토스트 메시지 표시
        if (isLeftEyeClosed && isRightEyeClosed) {
            Log.d(TAG, "두 눈이 감겼습니다. 토스트를 표시합니다.");
            Toast.makeText(this, "눈을 감았어요!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isEyeClosed(List<NormalizedLandmark> eyeLandmarks) {
        if (eyeLandmarks == null || eyeLandmarks.size() < 6) {
            return false;
        }
        float upperY = eyeLandmarks.get(1).y();
        float lowerY = eyeLandmarks.get(5).y();
        return (lowerY - upperY) < 0.03f;  // 기준 값으로 눈이 감긴 상태를 판단
    }


    // 왼쪽 눈 랜드마크 추출
    private List<NormalizedLandmark> getLeftEyeLandmarks(List<NormalizedLandmark> faceLandmarks) {
        return faceLandmarks.subList(362, 374); // 왼쪽 눈 랜드마크 범위 (MediaPipe 문서 참조)
    }

    // 오른쪽 눈 랜드마크 추출
    private List<NormalizedLandmark> getRightEyeLandmarks(List<NormalizedLandmark> faceLandmarks) {
        return faceLandmarks.subList(133, 145); // 오른쪽 눈 랜드마크 범위 (MediaPipe 문서 참조)
    }


} //MainActivity
