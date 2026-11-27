package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.transaction.TransactionConfirmResult;
import com.novabank.novapay.domain.repository.TransactionRepository;

import java.util.regex.Pattern;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public class ConfirmTransactionUseCase {

    private static final Pattern OTP_SIX_DIGITS = Pattern.compile("^\\d{6}$");

    private final TransactionRepository transactionRepository;

    @Inject
    public ConfirmTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Single<AppResult<TransactionConfirmResult>> execute(String draftId, String otp, boolean biometricPassed) {
        if (draftId == null || draftId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã phiên giao dịch không hợp lệ."));
        }
        String normalizedOtp = otp != null ? otp.trim() : "";
        if (!OTP_SIX_DIGITS.matcher(normalizedOtp).matches()) {
            return Single.just(AppResult.error("OTP_INVALID", "Mã OTP phải gồm đúng 6 chữ số."));
        }
        return transactionRepository.confirmTransaction(draftId.trim(), normalizedOtp, biometricPassed);
    }
}
