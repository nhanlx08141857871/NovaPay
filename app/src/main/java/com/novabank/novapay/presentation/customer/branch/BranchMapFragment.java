package com.novabank.novapay.presentation.customer.branch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.DashPathEffect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentBranchMapBinding;
import com.novabank.novapay.domain.model.branch.Branch;
import com.novabank.novapay.domain.model.branch.BranchType;
import com.novabank.novapay.domain.model.branch.GeoLocation;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BranchMapFragment extends BaseFragment<FragmentBranchMapBinding> {

    private static final GeoLocation TDTU_ANCHOR =
            new GeoLocation(Constants.TDTU_DEFAULT_LAT, Constants.TDTU_DEFAULT_LNG);

    private BranchMapViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    @Nullable
    private MapView mapView;
    @Nullable
    private LocationManager locationManager;
    @Nullable
    private LocationListener activeLocationListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable gpsTimeoutRunnable;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        if (!isAdded() || getView() == null) {
                            return;
                        }
                        Boolean fineGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarseGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                        if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                            fetchCurrentLocationWithTimeout();
                        } else {
                            applyTdtuFallback(true);
                        }
                    }
            );

    @NonNull
    @Override
    protected FragmentBranchMapBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentBranchMapBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(BranchMapViewModel.class);
        viewModel.setOpenRouteApiKey(getString(R.string.openroute_api_key));

        Context appCtx = requireContext().getApplicationContext();
        IConfigurationProvider osmConfig = Configuration.getInstance();
        osmConfig.setUserAgentValue(appCtx.getPackageName());
        File basePath = new File(appCtx.getFilesDir(), "osmdroid");
        File tileCache = new File(appCtx.getCacheDir(), "osmdroid/tiles");
        if (!basePath.exists()) {
            basePath.mkdirs();
        }
        if (!tileCache.exists()) {
            tileCache.mkdirs();
        }
        osmConfig.setOsmdroidBasePath(basePath);
        osmConfig.setOsmdroidTileCache(tileCache);

        FragmentBranchMapBinding b = getBinding();
        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> {
                    viewModel.loadBranches();
                    requestOrFetchLocation();
                }
        );
        b.stateEmpty.tvEmptyTitle.setText(R.string.map_branches_empty_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.map_branches_empty_subtitle);

        mapView = b.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(
                new GeoPoint(Constants.TDTU_DEFAULT_LAT, Constants.TDTU_DEFAULT_LNG)
        );

        b.toolbarBranchMap.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );
        b.toolbarBranchMap.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_my_location) {
                requestOrFetchLocation();
                return true;
            }
            if (item.getItemId() == R.id.action_refresh) {
                viewModel.loadBranches();
                requestOrFetchLocation();
                return true;
            }
            return false;
        });

        b.btnRecenterMap.setOnClickListener(v -> requestOrFetchLocation());

        requestOrFetchLocation();
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || !isAdded() || getView() == null) {
                return;
            }
            stateLayoutHelper.render(state);
            if (state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
                renderMap(state.getData());
            }
        });

        viewModel.getRouteLoading().observe(getViewLifecycleOwner(), loading -> {
            if (isAdded() && getView() != null) {
                getBinding().progressRouteLoading.setVisibility(
                        Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE
                );
            }
        });
    }

    private void requestOrFetchLocation() {
        if (!isAdded()) {
            return;
        }
        boolean fineGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            fetchCurrentLocationWithTimeout();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchCurrentLocationWithTimeout() {
        cancelLocationUpdates();
        if (!isAdded()) {
            return;
        }
        locationManager = requireContext().getSystemService(LocationManager.class);
        if (locationManager == null) {
            applyTdtuFallback(true);
            return;
        }

        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (IllegalArgumentException ignored) {
            // Provider unavailable on bare AOSP images
        }

        if (!gpsEnabled && !networkEnabled) {
            GeoLocation lastKnown = readValidLastKnownLocation();
            if (lastKnown != null) {
                viewModel.onUserLocationResolved(lastKnown, false);
            } else {
                applyTdtuFallback(true);
            }
            return;
        }

        activeLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (!isAdded() || getView() == null || mapView == null) {
                    return;
                }
                cancelLocationUpdates();
                GeoLocation actual = new GeoLocation(location.getLatitude(), location.getLongitude());
                if (actual.isBeyondRadius(TDTU_ANCHOR, Constants.MAX_VALID_DISTANCE_FROM_HCMC_METERS)) {
                    applyTdtuFallback(true);
                } else {
                    viewModel.onUserLocationResolved(actual, false);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };

        boolean requestedAny = false;
        if (gpsEnabled) {
            requestedAny |= tryRequestProvider(LocationManager.GPS_PROVIDER, activeLocationListener);
        }
        if (networkEnabled) {
            requestedAny |= tryRequestProvider(LocationManager.NETWORK_PROVIDER, activeLocationListener);
        }

        if (!requestedAny) {
            applyTdtuFallback(true);
            return;
        }

        gpsTimeoutRunnable = () -> {
            if (!isAdded() || getView() == null || mapView == null) {
                return;
            }
            cancelLocationUpdates();
            GeoLocation lastKnown = readValidLastKnownLocation();
            if (lastKnown != null) {
                viewModel.onUserLocationResolved(lastKnown, false);
            } else {
                applyTdtuFallback(true);
            }
        };
        mainHandler.postDelayed(gpsTimeoutRunnable, Constants.GPS_TIMEOUT_MILLIS);
    }

    private boolean tryRequestProvider(
            @NonNull String provider,
            @NonNull LocationListener listener
    ) {
        if (locationManager == null) {
            return false;
        }
        try {
            locationManager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
            );
            return true;
        } catch (SecurityException | IllegalArgumentException ex) {
            return false;
        }
    }

    @Nullable
    private GeoLocation readValidLastKnownLocation() {
        if (locationManager == null || !isAdded()) {
            return null;
        }
        try {
            Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location net = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location best = gps != null ? gps : net;
            if (best == null) {
                return null;
            }
            GeoLocation loc = new GeoLocation(best.getLatitude(), best.getLongitude());
            if (loc.isBeyondRadius(TDTU_ANCHOR, Constants.MAX_VALID_DISTANCE_FROM_HCMC_METERS)) {
                return null;
            }
            return loc;
        } catch (SecurityException | IllegalArgumentException ex) {
            return null;
        }
    }

    private void applyTdtuFallback(boolean showNotice) {
        if (!isAdded() || getView() == null) {
            return;
        }
        if (showNotice) {
            showSnackbar(getString(R.string.map_tdtu_fallback_notice));
        }
        viewModel.onUserLocationResolved(TDTU_ANCHOR, true);
    }

    private void renderMap(@NonNull BranchMapUiState state) {
        if (mapView == null || !isAdded() || getView() == null) {
            return;
        }

        mapView.getOverlays().clear();

        List<GeoLocation> routePoints = state.getRoutePoints();
        if (routePoints.size() >= 2) {
            Polyline routeLine = new Polyline(mapView);
            List<GeoPoint> geoPoints = new ArrayList<>(routePoints.size());
            for (GeoLocation pt : routePoints) {
                geoPoints.add(new GeoPoint(pt.getLat(), pt.getLng()));
            }
            routeLine.setPoints(geoPoints);
            routeLine.getOutlinePaint().setColor(
                    ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary)
            );
            routeLine.getOutlinePaint().setStrokeWidth(10f);
            if (state.isStraightLineFallback()) {
                routeLine.getOutlinePaint().setPathEffect(new DashPathEffect(new float[]{24f, 14f}, 0f));
            }
            mapView.getOverlays().add(routeLine);
        }

        GeoLocation userLoc = state.getUserLocation();
        Marker userMarker = new Marker(mapView);
        userMarker.setPosition(new GeoPoint(userLoc.getLat(), userLoc.getLng()));
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        userMarker.setTitle(getString(state.isUsingTdtuFallback()
                ? R.string.map_tdtu_marker_title
                : R.string.map_user_marker_title));
        mapView.getOverlays().add(userMarker);

        Branch selected = state.getSelectedBranch();
        for (Branch branch : state.getBranches()) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(
                    branch.getLocation().getLat(),
                    branch.getLocation().getLng()
            ));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(branch.getName());
            marker.setSnippet(branch.getAddress());
            marker.setOnMarkerClickListener((clickedMarker, map) -> {
                viewModel.selectBranch(branch);
                clickedMarker.showInfoWindow();
                return true;
            });
            mapView.getOverlays().add(marker);
            if (selected != null && selected.getBranchId().equals(branch.getBranchId())) {
                marker.showInfoWindow();
            }
        }

        mapView.getController().animateTo(new GeoPoint(userLoc.getLat(), userLoc.getLng()));
        mapView.invalidate();

        FragmentBranchMapBinding b = getBinding();
        if (selected != null) {
            b.cardBranchDetails.setVisibility(View.VISIBLE);
            b.tvBranchName.setText(selected.getName());
            b.tvBranchTypeBadge.setText(selected.getType() == BranchType.ATM
                    ? R.string.map_type_atm
                    : R.string.map_type_branch);
            boolean isNearest = state.getNearestBranch() != null
                    && state.getNearestBranch().getBranchId().equals(selected.getBranchId());
            b.tvNearestBadge.setVisibility(isNearest ? View.VISIBLE : View.GONE);
            b.tvBranchAddress.setText(selected.getAddress());
            b.tvBranchOpenHours.setText(getString(R.string.map_open_hours_format, selected.getOpenHours()));

            int etaMinutes = Math.max(1, (int) Math.ceil(state.getEstimatedDurationSeconds() / 60.0));
            double distanceMeters = state.getDistanceMeters();
            String distanceSummary = distanceMeters < 1000.0
                    ? getString(R.string.map_distance_meters_format, Math.round(distanceMeters), etaMinutes)
                    : getString(R.string.map_distance_km_format, distanceMeters / 1000.0, etaMinutes);
            if (state.isStraightLineFallback()) {
                distanceSummary = distanceSummary + " (" + getString(R.string.map_straight_line_badge) + ")";
            }
            b.tvBranchDistanceAndDuration.setText(distanceSummary);
        } else {
            b.cardBranchDetails.setVisibility(View.GONE);
        }
    }

    private void cancelLocationUpdates() {
        if (gpsTimeoutRunnable != null) {
            mainHandler.removeCallbacks(gpsTimeoutRunnable);
            gpsTimeoutRunnable = null;
        }
        if (locationManager != null && activeLocationListener != null) {
            try {
                locationManager.removeUpdates(activeLocationListener);
            } catch (SecurityException ignored) {
            }
            activeLocationListener = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        cancelLocationUpdates();
        if (mapView != null) {
            mapView.getOverlays().clear();
            mapView.onDetach();
            mapView = null;
        }
        super.onDestroyView();
    }
}
