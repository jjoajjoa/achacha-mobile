package com.achacha_mobile;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.location.Location;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.MediaPipeException;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

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
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    // 카메라 권한 요청 코드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    // MediaPipe - 객체인식
    private FaceLandmarker faceLandmarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //웹뷰 설정
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient()); // 링크 클릭 시 새 브라우저 열리지 않도록 설정
        webView.addJavascriptInterface(new WebAppInterface(this), "Android"); // 웹앱 인터페이스 안에 있는 함수를 실행 시킬 수 있음 - 일단 토스트로
        // 웹 설정
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // JavaScript 사용 가능하게 설정
        webView.loadUrl("http://172.168.30.184:8080/"); // 링크

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

        // 카메라 권한 체크
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // 권한이 이미 있으면 카메라 열기
            startCamera();
        }

        // MediaPipe 모델 초기화
        loadModel();

    } //onCreate

    // 카메라 권한 설정
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되었을 때 카메라 열기
                startCamera();
            } else {
                // 권한이 거부되었을 때 처리 (예: 알림, 종료 등)
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        Toast.makeText(this, "위치 업데이트 시작", Toast.LENGTH_SHORT).show();
        showStartNoti();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Toast.makeText(this, "위치 업데이트 중지", Toast.LENGTH_SHORT).show();
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

        ApiService apiService = retrofit.create(ApiService.class);
        GpsData gpsData = new GpsData(latitude, longitude, altitude, speed, accuracy, time);

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

    interface ApiService {
        @POST("/api/location/update")
        Call<Void> updateLocation(@Body GpsData location);
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
                Log.e(TAG, "FaceLandmarker is null after loading model.");
            } else {
                Log.d(TAG, "Model loaded successfully.");
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
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    if (faceLandmarker != null) {
                        Bitmap bitmap = imageToBitmap(image);
                        MPImage mpImage = new BitmapImageBuilder(bitmap).build();

                        // 얼굴 랜드마크 추출 및 분석
                        FaceLandmarkerResult result = faceLandmarker.detect(mpImage);
                        analyzeFaceLandmarks(result);

                        // 이미지 리소스 해제
                        image.close();
                    }
                });

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }


    // ImageProxy에서 YUV 이미지 데이터를 추출하고 이를 Bitmap으로 변환
    // YuvImage를 사용하여 NV21 포맷의 이미지를 JPEG로 변환하고, 이를 BitmapFactory로 디코딩하여 Bitmap 객체를 반환
    // 변환된 Bitmap은 얼굴 랜드마크를 추출하는 데 사용
    private Bitmap imageToBitmap(ImageProxy image) {
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
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    // 얼굴 랜드마크 분석 메서드
    private void analyzeFaceLandmarks(FaceLandmarkerResult result) {
        if (result.faceLandmarks().isEmpty()) {
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

    // 눈 감김 여부 확인 (눈꺼풀 사이의 Y 좌표 차이로 판단)
    private boolean isEyeClosed(List<NormalizedLandmark> eyeLandmarks) {
        float upperY = eyeLandmarks.get(1).y();
        float lowerY = eyeLandmarks.get(5).y();
        return (lowerY - upperY) < 0.03f;  // 눈이 감긴 상태를 판단하는 기준
    }

    // 왼쪽 눈 랜드마크 추출
    private List<NormalizedLandmark> getLeftEyeLandmarks(List<NormalizedLandmark> faceLandmarks) {
        return faceLandmarks.subList(362, 374);
    }

    // 오른쪽 눈 랜드마크 추출
    private List<NormalizedLandmark> getRightEyeLandmarks(List<NormalizedLandmark> faceLandmarks) {
        return faceLandmarks.subList(133, 145);
    }

} //MainActivity
