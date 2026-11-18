package com.novabank.novapay.domain.usecase.branch;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.branch.GeoLocation;
import com.novabank.novapay.domain.model.branch.WalkingRoute;
import com.novabank.novapay.domain.repository.BranchRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class GetWalkingRouteUseCase {

    private final BranchRepository branchRepository;

    @Inject
    public GetWalkingRouteUseCase(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public Single<AppResult<WalkingRoute>> execute(
            GeoLocation origin,
            GeoLocation destination,
            String apiKey
    ) {
        if (origin == null || destination == null) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Thiếu tọa độ điểm đầu hoặc điểm cuối."));
        }
        return branchRepository.getWalkingRoute(origin, destination, apiKey);
    }
}
