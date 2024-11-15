package com.achacha_mobile;

public class GeofenceData {
    private String id;  // 지오펜스 고유 ID
    private double latitude;  // 지오펜스 중심 위도
    private double longitude;  // 지오펜스 중심 경도
    private float radius;  // 지오펜스 반경

    // 생성자, getter, setter
    public GeofenceData(String id, double latitude, double longitude, float radius) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public String getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getRadius() {
        return radius;
    }
}
