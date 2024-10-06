package com.achacha_mobile.network;

import com.achacha_mobile.GpsData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/api/gps")  // 서버의 API 엔드포인트에 맞게 수정
    Call<Void> sendGpsData(@Body GpsData gpsData);
}