package com.novabank.novapay.data.repository;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.dto.OpenRouteDirectionsRequestDto;
import com.novabank.novapay.data.mapper.OpenRouteMapper;
import com.novabank.novapay.data.mapper.OfficerFirestoreMapper;
import com.novabank.novapay.data.remote.maps.OpenRouteServiceApi;
import com.novabank.novapay.domain.model.branch.Branch;
import com.novabank.novapay.domain.model.branch.GeoLocation;
import com.novabank.novapay.domain.model.branch.WalkingRoute;
import com.novabank.novapay.domain.repository.BranchRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public final class BranchRepositoryImpl implements BranchRepository {

    private final FirebaseFirestore firestore;
    private final OpenRouteServiceApi openRouteServiceApi;

    @Inject
    public BranchRepositoryImpl(
            FirebaseFirestore firestore,
            OpenRouteServiceApi openRouteServiceApi
    ) {
        this.firestore = firestore;
        this.openRouteServiceApi = openRouteServiceApi;
    }

    @Override
    public Single<AppResult<List<Branch>>> getAllBranches() {
        return Single.<AppResult<List<Branch>>>create(emitter -> firestore.collection("branches")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (emitter.isDisposed()) {
                        return;
                    }
                    List<Branch> branches = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Branch branch = OfficerFirestoreMapper.toBranch(doc);
                        if (branch != null) {
                            branches.add(branch);
                        }
                    }
                    branches.sort(Comparator.comparing(Branch::getName, String.CASE_INSENSITIVE_ORDER));
                    emitter.onSuccess(AppResult.success(branches));
                })
                .addOnFailureListener(error -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error(
                                "FIRESTORE_ERROR",
                                error.getMessage() != null
                                        ? error.getMessage()
                                        : "Không thể tải danh sách chi nhánh và ATM."
                        ));
                    }
                })).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<WalkingRoute>> getWalkingRoute(
            GeoLocation origin,
            GeoLocation destination,
            String apiKey
    ) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Single.just(AppResult.success(WalkingRoute.straightLine(origin, destination)));
        }
        OpenRouteDirectionsRequestDto requestDto =
                OpenRouteDirectionsRequestDto.fromLocations(origin, destination);
        return openRouteServiceApi.getWalkingDirections(apiKey.trim(), requestDto)
                .map(response -> {
                    WalkingRoute route = OpenRouteMapper.toWalkingRoute(response);
                    if (route == null) {
                        return AppResult.success(WalkingRoute.straightLine(origin, destination));
                    }
                    return AppResult.success(route);
                })
                .onErrorReturn(err -> AppResult.success(WalkingRoute.straightLine(origin, destination)))
                .subscribeOn(Schedulers.io());
    }
}
