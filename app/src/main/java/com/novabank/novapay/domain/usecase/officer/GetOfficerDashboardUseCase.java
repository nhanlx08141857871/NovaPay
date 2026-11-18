package com.novabank.novapay.domain.usecase.officer;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.officer.OfficerDashboardSummary;
import com.novabank.novapay.domain.repository.OfficerRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class GetOfficerDashboardUseCase {

    private final OfficerRepository officerRepository;

    @Inject
    public GetOfficerDashboardUseCase(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    public Single<AppResult<OfficerDashboardSummary>> execute() {
        return officerRepository.getDashboardSummary();
    }
}
