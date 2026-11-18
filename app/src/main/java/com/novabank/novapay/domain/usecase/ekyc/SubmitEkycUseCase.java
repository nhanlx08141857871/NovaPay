package com.novabank.novapay.domain.usecase.ekyc;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.EkycRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

/**
 * UseCase gửi ảnh khuôn mặt eKYC (< 200KB Base64) lên máy chủ.
 */
public final class SubmitEkycUseCase {

    private final EkycRepository ekycRepository;

    @Inject
    public SubmitEkycUseCase(EkycRepository ekycRepository) {
        this.ekycRepository = ekycRepository;
    }

    public Single<AppResult<String>> execute(String imageBase64, String targetUid) {
        if (imageBase64 == null || imageBase64.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Ảnh khuôn mặt eKYC không hợp lệ."));
        }
        String normalizedTarget = (targetUid != null && !targetUid.trim().isEmpty())
                ? targetUid.trim()
                : null;
        return ekycRepository.submitEkyc(imageBase64.trim(), normalizedTarget);
    }
}
