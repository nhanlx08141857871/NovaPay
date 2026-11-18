package com.novabank.novapay.domain.model.branch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class WalkingRoute {

    private static final double DEFAULT_WALKING_SPEED_METERS_PER_SECOND = 1.33;

    private final List<GeoLocation> points;
    private final double distanceMeters;
    private final double durationSeconds;
    private final boolean straightLineFallback;

    public WalkingRoute(
            List<GeoLocation> points,
            double distanceMeters,
            double durationSeconds,
            boolean straightLineFallback
    ) {
        this.points = points == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(points));
        this.distanceMeters = Math.max(0.0, distanceMeters);
        this.durationSeconds = Math.max(0.0, durationSeconds);
        this.straightLineFallback = straightLineFallback;
    }

    public static WalkingRoute straightLine(GeoLocation origin, GeoLocation destination) {
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        double distance = origin.distanceMetersTo(destination);
        double duration = distance / DEFAULT_WALKING_SPEED_METERS_PER_SECOND;
        List<GeoLocation> segment = new ArrayList<>(2);
        segment.add(origin);
        segment.add(destination);
        return new WalkingRoute(segment, distance, duration, true);
    }

    public List<GeoLocation> getPoints() {
        return points;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isStraightLineFallback() {
        return straightLineFallback;
    }
}
