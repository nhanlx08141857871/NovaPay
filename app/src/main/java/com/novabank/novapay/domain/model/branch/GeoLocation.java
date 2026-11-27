package com.novabank.novapay.domain.model.branch;

import java.util.Objects;

public final class GeoLocation {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final double lat;
    private final double lng;

    public GeoLocation(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double distanceMetersTo(GeoLocation other) {
        Objects.requireNonNull(other, "other GeoLocation must not be null");
        double dLat = Math.toRadians(other.lat - this.lat);
        double dLng = Math.toRadians(other.lng - this.lng);
        double radLat1 = Math.toRadians(this.lat);
        double radLat2 = Math.toRadians(other.lat);

        double sinHalfLat = Math.sin(dLat / 2.0);
        double sinHalfLng = Math.sin(dLng / 2.0);

        double a = sinHalfLat * sinHalfLat
                + Math.cos(radLat1) * Math.cos(radLat2) * sinHalfLng * sinHalfLng;
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1.0 - a)));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Kiểm tra tọa độ hiện tại có nằm quá xa cụm chi nhánh chuẩn hay không
     * (dùng để kích hoạt fallback về ĐH Tôn Đức Thắng khi máy ảo mặc định ở nước ngoài > 100km).
     */
    public boolean isBeyondRadius(GeoLocation center, double maxRadiusMeters) {
        return distanceMetersTo(center) > maxRadiusMeters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeoLocation that)) return false;
        return Double.compare(that.lat, lat) == 0 && Double.compare(that.lng, lng) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lng);
    }

    @Override
    public String toString() {
        return "GeoLocation{lat=" + lat + ", lng=" + lng + '}';
    }
}
