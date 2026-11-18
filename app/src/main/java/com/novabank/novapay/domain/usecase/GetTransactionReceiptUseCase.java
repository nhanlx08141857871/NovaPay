package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.repository.TransactionRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

/**
 * Tải chi tiết giao dịch từ transactions/{txId} để hiển thị và xuất ảnh biên lai PNG.
 */
public class GetTransactionReceiptUseCase {

    private final TransactionRepository transactionRepository;

    @Inject
    public GetTransactionReceiptUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Single<AppResult<Transaction>> execute(String txId) {
        if (txId == null || txId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã giao dịch không hợp lệ."));
        }
        return transactionRepository.getTransactionById(txId.trim());
    }
}
