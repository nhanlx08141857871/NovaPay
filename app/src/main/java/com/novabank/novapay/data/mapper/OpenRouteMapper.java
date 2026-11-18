package com.novabank.novapay.data.mapper;

import androidx.annotation.Nullable;

import com.novabank.novapay.data.dto.OpenRouteDirectionsResponseDto;
import com.novabank.novapay.domain.model.branch.GeoLocation;
import com.novabank.novapay.domain.model.branch.WalkingRoute;

import java.util.ArrayList;
import java.util.List;

public final class OpenRouteMapper {

    private OpenRouteMapper() {
    }

    @Nullable
    public static WalkingRoute toWalkingRoute(@Nullable OpenRouteDirectionsResponseDto response) {
        if (response == null || response.getFeatures() == null || response.getFeatures().isEmpty()) {
            return null;
        }
        OpenRouteDirectionsResponseDto.FeatureDto feature = response.getFeatures().get(0);
        if (feature == null || feature.getGeometry() == null
                || feature.getGeometry().getCoordinates() == null) {
            return null;
        }

        List<GeoLocation> points = new ArrayList<>();
        for (List<Double> coord : feature.getGeometry().getCoordinates()) {
            if (coord != null && coord.size() >= 2 && coord.get(0) != null && coord.get(1) != null) {
                double lng = coord.get(0);
                double lat = coord.get(1);
                points.add(new GeoLocation(lat, lng));
            }
        }
        if (points.size() < 2) {
            return null;
        }

        double distance = 0.0;
        double duration = 0.0;
        if (feature.getProperties() != null && feature.getProperties().getSummary() != null) {
            distance = feature.getProperties().getSummary().getDistance();
            duration = feature.getProperties().getSummary().getDuration();
        } else {
            for (int i = 1; i < points.size(); i++) {
                distance += points.get(i - 1).distanceMetersTo(points.get(i));
            }
            duration = distance / 1.33;
        }

        return new WalkingRoute(points, distance, duration, false);
    }
}
