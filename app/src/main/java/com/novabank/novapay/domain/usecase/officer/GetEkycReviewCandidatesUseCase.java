package com.novabank.novapay.domain.usecase.officer;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.officer.EkycReviewCandidate;
import com.novabank.novapay.domain.repository.OfficerRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class GetEkycReviewCandidatesUseCase {

    private final OfficerRepository officerRepository;

    @Inject
    public GetEkycReviewCandidatesUseCase(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    public Single<AppResult<List<EkycReviewCandidate>>> execute() {
        return officerRepository.getEkycReviewCandidates();
    }
}
