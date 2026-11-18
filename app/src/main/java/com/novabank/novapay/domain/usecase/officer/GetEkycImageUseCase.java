package com.novabank.novapay.domain.usecase.officer;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.OfficerRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class GetEkycImageUseCase {

    private final OfficerRepository officerRepository;

    @Inject
    public GetEkycImageUseCase(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    public Single<AppResult<String>> execute(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Thiếu mã khách hàng."));
        }
        return officerRepository.getEkycImageBase64(uid.trim());
    }
}
