package com.novabank.novapay.domain.usecase.branch;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.branch.Branch;
import com.novabank.novapay.domain.repository.BranchRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class GetAllBranchesUseCase {

    private final BranchRepository branchRepository;

    @Inject
    public GetAllBranchesUseCase(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public Single<AppResult<List<Branch>>> execute() {
        return branchRepository.getAllBranches();
    }
}
