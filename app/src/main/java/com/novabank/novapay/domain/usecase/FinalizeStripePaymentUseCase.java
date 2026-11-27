package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.transaction.PaymentFinalizeResult;
import com.novabank.novapay.domain.repository.TransactionRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public class FinalizeStripePaymentUseCase {

    private final TransactionRepository transactionRepository;

    @Inject
    public FinalizeStripePaymentUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Single<AppResult<PaymentFinalizeResult>> execute(String draftId) {
        if (draftId == null || draftId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã phiên nạp tiền không hợp lệ."));
        }
        return transactionRepository.finalizeStripePayment(draftId.trim());
    }
}
