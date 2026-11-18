package com.novabank.novapay.presentation.customer.branch;

import androidx.annotation.Nullable;

import com.novabank.novapay.domain.model.branch.Branch;
import com.novabank.novapay.domain.model.branch.GeoLocation;

import java.util.Collections;
import java.util.List;

public final class BranchMapUiState {

    private final List<Branch> branches;
    private final GeoLocation userLocation;
    private final boolean usingTdtuFallback;
    @Nullable
    private final Branch selectedBranch;
    @Nullable
    private final Branch nearestBranch;
    private final double distanceMeters;
    private final double estimatedDurationSeconds;
    private final List<GeoLocation> routePoints;
    private final boolean straightLineFallback;

    public BranchMapUiState(
            List<Branch> branches,
            GeoLocation userLocation,
            boolean usingTdtuFallback,
            @Nullable Branch selectedBranch,
            @Nullable Branch nearestBranch,
            double distanceMeters,
            double estimatedDurationSeconds,
            List<GeoLocation> routePoints,
            boolean straightLineFallback
    ) {
        this.branches = branches != null ? branches : Collections.emptyList();
        this.userLocation = userLocation;
        this.usingTdtuFallback = usingTdtuFallback;
        this.selectedBranch = selectedBranch;
        this.nearestBranch = nearestBranch;
        this.distanceMeters = distanceMeters;
        this.estimatedDurationSeconds = estimatedDurationSeconds;
        this.routePoints = routePoints != null ? routePoints : Collections.emptyList();
        this.straightLineFallback = straightLineFallback;
    }

    public List<Branch> getBranches() {
        return branches;
    }

    public GeoLocation getUserLocation() {
        return userLocation;
    }

    public boolean isUsingTdtuFallback() {
        return usingTdtuFallback;
    }

    @Nullable
    public Branch getSelectedBranch() {
        return selectedBranch;
    }

    @Nullable
    public Branch getNearestBranch() {
        return nearestBranch;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public double getEstimatedDurationSeconds() {
        return estimatedDurationSeconds;
    }

    public List<GeoLocation> getRoutePoints() {
        return routePoints;
    }

    public boolean isStraightLineFallback() {
        return straightLineFallback;
    }
}
