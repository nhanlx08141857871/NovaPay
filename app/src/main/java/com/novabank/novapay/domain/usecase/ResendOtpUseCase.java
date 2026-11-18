package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.TransactionRepository;

import java.time.Instant;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

/**
 * Gửi lại mã OTP Email 6 chữ số cho bản nháp giao dịch hiện tại.
 */
public class ResendOtpUseCase {

    private final TransactionRepository transactionRepository;

    @Inject
    public ResendOtpUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Single<AppResult<Instant>> execute(String draftId) {
        if (draftId == null || draftId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã phiên giao dịch không hợp lệ."));
        }
        return transactionRepository.resendOtp(draftId.trim());
    }
}
