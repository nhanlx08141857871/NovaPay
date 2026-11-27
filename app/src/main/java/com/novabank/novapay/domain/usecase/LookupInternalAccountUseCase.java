package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.transaction.AccountLookupResult;
import com.novabank.novapay.domain.repository.TransactionRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public class LookupInternalAccountUseCase {

    private final TransactionRepository transactionRepository;

    @Inject
    public LookupInternalAccountUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Single<AppResult<AccountLookupResult>> execute(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Vui lòng nhập số tài khoản người nhận."));
        }
        String normalized = accountNumber.trim();
        if (normalized.contains(" ")) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Số tài khoản không được chứa khoảng trắng."));
        }
        return transactionRepository.lookupInternalAccount(normalized);
    }
}
