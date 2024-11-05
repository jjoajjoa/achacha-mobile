package com.achacha_mobile;

import com.google.gson.annotations.SerializedName;

public class GpsData {
    @SerializedName("gpsLatitude")
    private Double latitude; // 위도

    @SerializedName("gpsLongitude")
    private Double longitude; // 경도

    @SerializedName("gpsAltitude")
    private Double altitude; // 고도

    @SerializedName("gpsSpeed")
    private Double speed;    // 속도

    @SerializedName("gpsAccuracy")
    private Double accuracy; // 정확도

    @SerializedName("gpsLogTime")
    private String time; // 시간

    @SerializedName("employeeId")
    private String employeeId; // 유저 id

    // Double을 사용하는 이유
    // null 값을 허용하지 않는 경우에는 double을 사용해야 하지만, API 요청을 위해 null을 허용해야 한다면 Double로 변경하는 것이 더 적합

    // 생성자
    public GpsData(Double latitude, Double longitude, String time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public GpsData(Double latitude, Double longitude, Double altitude, Double speed, Double accuracy, String time, String userId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.accuracy = accuracy;
        this.time = time;
        this.employeeId = userId;
    }

    // Getter와 Setter
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setEmployeeId(String employeeId) {this.employeeId = employeeId;}

    public String getEmployeeId() {return employeeId;}
}
