package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.branch.Branch;
import com.novabank.novapay.domain.model.branch.GeoLocation;
import com.novabank.novapay.domain.model.branch.WalkingRoute;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public interface BranchRepository {

    Single<AppResult<List<Branch>>> getAllBranches();

    Single<AppResult<WalkingRoute>> getWalkingRoute(
            GeoLocation origin,
            GeoLocation destination,
            String apiKey
    );
}
