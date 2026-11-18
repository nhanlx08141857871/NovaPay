package com.novabank.novapay.domain.usecase.officer;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.domain.repository.OfficerRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class ReviewEkycUseCase {

    private final OfficerRepository officerRepository;

    @Inject
    public ReviewEkycUseCase(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    public Single<AppResult<String>> execute(String uid, String decision) {
        if (uid == null || uid.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Thiếu mã khách hàng."));
        }
        if (!Constants.EKYC_STATUS_VERIFIED.equals(decision)
                && !Constants.EKYC_STATUS_REJECTED.equals(decision)) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Quyết định duyệt eKYC không hợp lệ."));
        }
        return officerRepository.reviewEkyc(uid.trim(), decision);
    }
}
