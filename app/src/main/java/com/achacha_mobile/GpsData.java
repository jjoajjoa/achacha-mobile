package com.achacha_mobile;

public class GpsData {
    private double latitude;
    private double longitude;
    private double altitude; // 고도는 선택적일 수 있음
    private double speed;    // 속도도 선택적일 수 있음
    private double accuracy; // 정확도
    private String time; // 방향

    // 생성자
    public GpsData(double latitude, double longitude, double altitude, double speed, double accuracy, String time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.accuracy = accuracy;
        this.time = time;
    }

    // Getter와 Setter
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
