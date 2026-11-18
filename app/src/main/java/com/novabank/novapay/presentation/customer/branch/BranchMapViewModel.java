package com.novabank.novapay.presentation.customer.branch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.branch.Branch;
import com.novabank.novapay.domain.model.branch.GeoLocation;
import com.novabank.novapay.domain.model.branch.WalkingRoute;
import com.novabank.novapay.domain.usecase.branch.FindNearestBranchUseCase;
import com.novabank.novapay.domain.usecase.branch.GetAllBranchesUseCase;
import com.novabank.novapay.domain.usecase.branch.GetWalkingRouteUseCase;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

@HiltViewModel
public class BranchMapViewModel extends ViewModel {

    private final GetAllBranchesUseCase getAllBranchesUseCase;
    private final FindNearestBranchUseCase findNearestBranchUseCase;
    private final GetWalkingRouteUseCase getWalkingRouteUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();
    @Nullable
    private Disposable routeDisposable;

    private final MutableLiveData<UiState<BranchMapUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<Boolean> routeLoading = new MutableLiveData<>(false);

    private List<Branch> loadedBranches = Collections.emptyList();
    private GeoLocation currentUserLocation =
            new GeoLocation(Constants.TDTU_DEFAULT_LAT, Constants.TDTU_DEFAULT_LNG);
    private boolean usingTdtuFallback = true;
    @Nullable
    private Branch nearestBranch;
    @Nullable
    private Branch selectedBranch;
    private String openRouteApiKey = "";

    @Inject
    public BranchMapViewModel(
            GetAllBranchesUseCase getAllBranchesUseCase,
            FindNearestBranchUseCase findNearestBranchUseCase,
            GetWalkingRouteUseCase getWalkingRouteUseCase
    ) {
        this.getAllBranchesUseCase = getAllBranchesUseCase;
        this.findNearestBranchUseCase = findNearestBranchUseCase;
        this.getWalkingRouteUseCase = getWalkingRouteUseCase;
        loadBranches();
    }

    public LiveData<UiState<BranchMapUiState>> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getRouteLoading() {
        return routeLoading;
    }

    public void setOpenRouteApiKey(String apiKey) {
        this.openRouteApiKey = apiKey != null ? apiKey : "";
    }

    public void loadBranches() {
        uiState.setValue(UiState.loading());
        disposables.add(
                getAllBranchesUseCase.execute()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::handleBranchesResult,
                                error -> uiState.setValue(UiState.error(error.getMessage()))
                        )
        );
    }

    public void onUserLocationResolved(@NonNull GeoLocation location, boolean isFallback) {
        this.currentUserLocation = location;
        this.usingTdtuFallback = isFallback;
        if (!loadedBranches.isEmpty()) {
            this.nearestBranch = findNearestBranchUseCase.findNearest(location, loadedBranches);
            if (this.selectedBranch == null) {
                this.selectedBranch = this.nearestBranch;
            }
            requestRouteForSelectedBranch();
        }
    }

    public void selectBranch(@NonNull Branch branch) {
        this.selectedBranch = branch;
        requestRouteForSelectedBranch();
    }

    private void handleBranchesResult(AppResult<List<Branch>> result) {
        if (result == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        if (result.isError()) {
            uiState.setValue(UiState.error(result.getMessage()));
            return;
        }
        List<Branch> branches = result.getData();
        if (branches == null || branches.isEmpty()) {
            loadedBranches = Collections.emptyList();
            uiState.setValue(UiState.empty());
            return;
        }
        loadedBranches = branches;
        nearestBranch = findNearestBranchUseCase.findNearest(currentUserLocation, loadedBranches);
        selectedBranch = nearestBranch != null ? nearestBranch : loadedBranches.get(0);
        requestRouteForSelectedBranch();
    }

    private void requestRouteForSelectedBranch() {
        if (loadedBranches.isEmpty() || selectedBranch == null) {
            return;
        }
        if (routeDisposable != null && !routeDisposable.isDisposed()) {
            routeDisposable.dispose();
        }
        Branch target = selectedBranch;
        WalkingRoute initialFallback = WalkingRoute.straightLine(currentUserLocation, target.getLocation());
        emitMapSuccess(target, initialFallback);

        routeLoading.setValue(true);
        routeDisposable = getWalkingRouteUseCase.execute(
                        currentUserLocation,
                        target.getLocation(),
                        openRouteApiKey
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        routeResult -> {
                            routeLoading.setValue(false);
                            WalkingRoute route = routeResult.isSuccess() && routeResult.getData() != null
                                    ? routeResult.getData()
                                    : initialFallback;
                            emitMapSuccess(target, route);
                        },
                        error -> {
                            routeLoading.setValue(false);
                            emitMapSuccess(target, initialFallback);
                        }
                );
        disposables.add(routeDisposable);
    }

    private void emitMapSuccess(@NonNull Branch target, @NonNull WalkingRoute route) {
        uiState.setValue(UiState.success(new BranchMapUiState(
                loadedBranches,
                currentUserLocation,
                usingTdtuFallback,
                target,
                nearestBranch,
                route.getDistanceMeters(),
                route.getDurationSeconds(),
                route.getPoints(),
                route.isStraightLineFallback()
        )));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
