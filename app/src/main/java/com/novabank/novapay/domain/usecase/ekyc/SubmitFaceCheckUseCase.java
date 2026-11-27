package com.novabank.novapay.domain.usecase.ekyc;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.EkycRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class SubmitFaceCheckUseCase {

    private final EkycRepository ekycRepository;

    @Inject
    public SubmitFaceCheckUseCase(EkycRepository ekycRepository) {
        this.ekycRepository = ekycRepository;
    }

    public Single<AppResult<Long>> execute(String draftId, String imageBase64) {
        if (draftId == null || draftId.trim().isEmpty()
                || imageBase64 == null || imageBase64.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Thiếu mã phiên giao dịch hoặc ảnh khuôn mặt."));
        }
        return ekycRepository.submitFaceCheck(draftId.trim(), imageBase64.trim());
    }
}
