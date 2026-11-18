package com.novabank.novapay.data.mapper;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.GeoPoint;
import com.novabank.novapay.domain.model.branch.GeoLocation;

public final class GeoPointMapper {

    private GeoPointMapper() {
    }

    @Nullable
    public static GeoLocation toDomain(@Nullable GeoPoint geoPoint) {
        if (geoPoint == null) {
            return null;
        }
        return new GeoLocation(geoPoint.getLatitude(), geoPoint.getLongitude());
    }

    @Nullable
    public static GeoPoint toGeoPoint(@Nullable GeoLocation location) {
        if (location == null) {
            return null;
        }
        return new GeoPoint(location.getLat(), location.getLng());
    }
}
