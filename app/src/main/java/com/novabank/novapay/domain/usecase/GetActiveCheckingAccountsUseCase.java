package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.repository.SourceAccountRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class GetActiveCheckingAccountsUseCase {

    private final SourceAccountRepository sourceAccountRepository;

    @Inject
    public GetActiveCheckingAccountsUseCase(SourceAccountRepository sourceAccountRepository) {
        this.sourceAccountRepository = sourceAccountRepository;
    }

    public Observable<AppResult<List<Account>>> observe() {
        return sourceAccountRepository.observeActiveCheckingAccounts()
                .map(GetActiveCheckingAccountsUseCase::filterPaymentSources);
    }

    public Single<AppResult<List<Account>>> getOnce() {
        return sourceAccountRepository.getActiveCheckingAccounts()
                .map(GetActiveCheckingAccountsUseCase::filterPaymentSources);
    }

    private static AppResult<List<Account>> filterPaymentSources(AppResult<List<Account>> result) {
        if (result == null || result.isError()) {
            return result != null ? result : AppResult.error("UNKNOWN_ERROR", "Không thể tải tài khoản nguồn.");
        }
        List<Account> filtered = new ArrayList<>();
        if (result.getData() != null) {
            for (Account account : result.getData()) {
                if (account != null && account.canBePaymentSource()) {
                    filtered.add(account);
                }
            }
        }
        return AppResult.success(filtered);
    }
}
